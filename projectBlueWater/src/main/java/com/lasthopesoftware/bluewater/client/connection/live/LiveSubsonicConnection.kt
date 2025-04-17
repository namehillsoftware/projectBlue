package com.lasthopesoftware.bluewater.client.connection.live

import androidx.annotation.Keep
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.google.gson.JsonParser
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.policies.caching.TimedExpirationPromiseCache
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.InvalidResponseCodeException
import com.lasthopesoftware.resources.io.NonStandardResponseException
import com.lasthopesoftware.resources.strings.GetStringResources
import com.lasthopesoftware.resources.strings.TranslateJson
import com.lasthopesoftware.resources.strings.parseJson
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.joda.time.Duration
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

class LiveSubsonicConnection(
	private val subsonicConnectionDetails: SubsonicConnectionDetails,
	private val httpPromiseClients: ProvideHttpPromiseClients,
	private val okHttpClients: ProvideOkHttpClients,
	private val jsonTranslator: TranslateJson,
	private val stringResources: GetStringResources,
) : LiveServerConnection, RemoteLibraryAccess
{
	companion object {
		private val logger by lazyLogger<LiveSubsonicConnection>()
		private const val imageFormat = "jpg"
		private const val musicFormat = "mp3"
		private const val bitrate = "128"

		private const val playlistsItemKey = "playlists"
		private const val playlistItemKey = "playlist"
		private const val artistsItemKey = "artists"

		val playlistsItem = ItemId(playlistsItemKey)
		val artistsItem = ItemId(artistsItemKey)

		private val checkedExpirationTime by lazy { Duration.standardSeconds(30) }
	}

	private object KnownFileProperties {
		const val title = "title"
		const val id = "id"
		const val artist = "artist"
	}

	private val promisedRootItem by lazy {
		Promise(
			listOf<IItem>(
				Item(artistsItemKey, stringResources.artists, null),
				Item(playlistsItemKey, stringResources.playlists, null),
			)
		)
	}

	private val subsonicApiUrl by lazy { subsonicConnectionDetails.baseUrl.withSubsonicApi() }

	private val revisionCache by lazy { TimedExpirationPromiseCache<Unit, Int?>(checkedExpirationTime) }

	private val httpClient by lazy { httpPromiseClients.getServerClient(subsonicConnectionDetails) }

	private val cachedVersion by RetryOnRejectionLazyPromise {
		PingViewPromise().cancelBackThen { r, _ ->
			r?.version?.split(".")?.let {
				SemanticVersion(it[0].toInt(), it[1].toInt(), it[2].toInt())
			}
		}
	}

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(subsonicConnectionDetails.baseUrl, key)

	override fun getFileUrl(serviceFile: ServiceFile): URL =
		subsonicApiUrl
			.addPath("stream")
			.addParams(
				"id=${serviceFile.key}",
				"format=$musicFormat",
				"maxBitRate=$bitrate",
			)

	override val dataSourceFactory by lazy {
		OkHttpDataSource.Factory(
			okHttpClients
				.getOkHttpClient(subsonicConnectionDetails)
				.newBuilder()
				.readTimeout(45, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
				.build())
	}

	override val dataAccess: RemoteLibraryAccess
		get() = this

	override fun promiseIsConnectionPossible(): Promise<Boolean> = ConnectionPossiblePromise()

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> = FilePropertiesPromise(serviceFile)

	override fun promiseFilePropertyUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		Unit.toPromise()

	override fun promiseItems(itemId: KeyedIdentifier?): Promise<List<IItem>> = when (itemId) {
		null -> promisedRootItem
		artistsItem -> RootIndexPromise()
		playlistsItem -> RootPlaylistsPromise()
		is ItemId -> ItemPromise(itemId)
		else -> emptyList<IItem>().toPromise()
	}

	override fun promiseAudioPlaylistPaths(): Promise<List<String>> = Promise(emptyList())

	override fun promiseStoredPlaylist(playlistPath: String, playlist: List<ServiceFile>): Promise<*> = Unit.toPromise()

	override fun promiseIsReadOnly(): Promise<Boolean> = true.toPromise()

	override fun promiseServerVersion(): Promise<SemanticVersion?> = cachedVersion

	override fun promiseFile(serviceFile: ServiceFile): Promise<InputStream> = Promise.Proxy { cp ->
		httpClient
			.promiseResponse(getFileUrl(serviceFile))
			.also(cp::doCancel)
			.then(HttpStreamedResponse())
	}

	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> = emptyByteArray.toPromise()

	override fun promiseImageBytes(itemId: ItemId): Promise<ByteArray> = emptyByteArray.toPromise()

	override fun promiseFileStringList(itemId: ItemId?): Promise<String> = itemId
		?.let(::ItemFilesPromise)
		?.cancelBackEventually(FileStringListUtilities::promiseSerializedFileStringList)
		.keepPromise("")

	override fun promiseShuffledFileStringList(itemId: ItemId?): Promise<String> = "".toPromise()

	override fun promiseFiles(): Promise<List<ServiceFile>> = Promise(emptyList())

	override fun promiseFiles(query: String): Promise<List<ServiceFile>> = Promise(emptyList())

	override fun promiseFiles(itemId: ItemId): Promise<List<ServiceFile>> = ItemFilesPromise(itemId)

	override fun promiseFiles(playlistId: PlaylistId): Promise<List<ServiceFile>> = PlaylistFilesPromise(playlistId)

	override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> = promiseResponse(
		"scrobble",
		"id=${serviceFile.key}"
	).cancelBackThen { response, _ ->
		response.use {
			val responseCode = it.code
			logger.debug("rest/scrobble responded with a response code of {}", responseCode)
			if (responseCode < 200 || responseCode >= 300) throw HttpResponseException(responseCode)
		}
	}

	override fun promiseRevision(): Promise<Long?> = RevisionPromise()

	private fun promiseResponse(path: String, vararg params: String): Promise<HttpResponse> {
		val url = subsonicApiUrl.addPath(path).addParams(*params)
		return httpClient.promiseResponse(url)
	}

	private inline fun <reified T> Promise<HttpResponse>.promiseSubsonicResponse(): Promise<T?> = eventually { r ->
		r.promiseSubsonicResponse<T>()
	}

	private inline fun <reified T> HttpResponse.promiseSubsonicResponse(): Promise<T?> =
		ThreadPools.compute.preparePromise { cs ->
			if (cs.isCancelled) throw CancellationException("Cancelled before parsing response.")
			parseSubsonicResponse()
		}

	private inline fun <reified T> HttpResponse.parseSubsonicResponse(): T? {
		body.use {
			it.reader().use { r ->
				val json = JsonParser.parseReader(r).asJsonObject.get("subsonic-response") ?: throw NonStandardResponseException()
				return jsonTranslator.parseJson<T>(json)
			}
		}
	}

	private inner class FilePropertiesPromise(private val serviceFile: ServiceFile) :
		Promise.Proxy<Map<String, String>>()
	{
		init {
			proxy(
				promiseResponse("getSong", "id=${serviceFile.key}")
					.also(::doCancel)
					.eventually({ httpResponse ->
						ThreadPools.compute.preparePromise { cs ->
							if (cs.isCancelled) {
								throw filePropertiesCancellationException(serviceFile)
							}

							httpResponse.body.use { b ->
								b.reader().use { r ->
									val subsonicResponse = JsonParser.parseReader(r).asJsonObject.get("subsonic-response")

									val song = subsonicResponse?.asJsonObject?.get("song")

									song
										?.asJsonObject
										?.asMap()
										?.mapNotNull { (k, v) -> if (v.isJsonPrimitive) Pair(k, v.asString) else null }
										?.toMap()
										?.toSortedMap(String.CASE_INSENSITIVE_ORDER)
										?.also {
											it[NormalizedFileProperties.Key] = it[KnownFileProperties.id]
											it[NormalizedFileProperties.Name] = it[KnownFileProperties.title]
										}
										?: mutableMapOf()
								}
							}
						}
					}, ::handleException)
					.also(::doCancel)
					.eventually({ props ->
						promiseResponse(
							"getLyrics",
							"artist=${props[KnownFileProperties.artist]}",
							"title=${props[KnownFileProperties.title]}"
						)
							.also(::doCancel)
							.promiseSubsonicResponse<SubsonicLyricsResponse>()
							.also(::doCancel)
							.then { l ->
								props[NormalizedFileProperties.Lyrics] = l?.lyrics?.value ?: ""
								props
							}
					}, ::handleException)
			)
		}

		fun <T> handleException(rejection: Throwable): Promise<T> =
			if (isCancelled && rejection is IOException && rejection.isOkHttpCanceled()) {
				Promise(filePropertiesCancellationException(serviceFile))
			} else {
				Promise(rejection)
			}

		private fun filePropertiesCancellationException(serviceFile: ServiceFile) =
			CancellationException("Getting file properties cancelled for $serviceFile.")
	}

	private inner class RevisionPromise :
		Promise.Proxy<Long?>(),
		ImmediateResponse<SubsonicIndexesLastModifiedResponse?, Long?>
	{
		init {
			proxy(
				promiseResponse("getIndexes")
					.also(::doCancel)
					.promiseSubsonicResponse<SubsonicIndexesLastModifiedResponse>()
					.also(::doCancel)
					.then(this)
			)
		}

		override fun respond(resolution: SubsonicIndexesLastModifiedResponse?): Long? = resolution?.indexes?.lastModified
	}

	private inner class RootIndexPromise :
		Promise.Proxy<List<IItem>>(),
		PromisedResponse<SubsonicIndexesResponse?, List<IItem>>
	{
		init {
			proxy(
				promiseResponse("getIndexes")
					.also(::doCancel)
					.promiseSubsonicResponse<SubsonicIndexesResponse>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: SubsonicIndexesResponse?): Promise<List<IItem>> = ThreadPools.compute.preparePromise { cs ->
			response?.indexes?.index?.flatMap {
				it.artist.map { artist ->
					if (cs.isCancelled) throw itemParsingCancelledException()

					Item(artist.id, artist.name, playlistId = null)
				}
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class ItemPromise(itemId: ItemId) :
		Promise.Proxy<List<IItem>>(),
		PromisedResponse<SubsonicDirectoryRoot?, List<IItem>>
	{
		init {
			proxy(
				promiseResponse(
					"getMusicDirectory",
					"id=${itemId.id}"
				)
					.also(::doCancel)
					.promiseSubsonicResponse<SubsonicDirectoryRoot>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: SubsonicDirectoryRoot?): Promise<List<IItem>> = ThreadPools.compute.preparePromise { cs ->
			response?.directory?.child?.filter { it.isDir }?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				Item(it.id, it.name, playlistId = null)
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class RootPlaylistsPromise :
		Promise.Proxy<List<IItem>>(),
		PromisedResponse<SubsonicPlaylistsResponse?, List<IItem>>
	{
		init {
			proxy(
				promiseResponse("getPlaylists")
					.also(::doCancel)
					.promiseSubsonicResponse<SubsonicPlaylistsResponse>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: SubsonicPlaylistsResponse?): Promise<List<IItem>> = ThreadPools.compute.preparePromise { cs ->
			response?.playlists?.playlist?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				Item(playlistItemKey, it.name, PlaylistId(it.id))
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class ItemFilesPromise(itemId: ItemId) :
		Promise.Proxy<List<ServiceFile>>(),
		PromisedResponse<SubsonicDirectoryRoot?, List<ServiceFile>>
	{
		init {
			proxy(
				promiseResponse(
					"getMusicDirectory",
					"id=${itemId.id}"
				)
					.also(::doCancel)
					.promiseSubsonicResponse<SubsonicDirectoryRoot>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: SubsonicDirectoryRoot?): Promise<List<ServiceFile>> = ThreadPools.compute.preparePromise { cs ->
			response?.directory?.child?.filter { !it.isDir }?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				ServiceFile(it.id)
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class PlaylistFilesPromise(playlistId: PlaylistId) :
		Promise.Proxy<List<ServiceFile>>(),
		PromisedResponse<SubsonicPlaylistResponse?, List<ServiceFile>>
	{
		init {
			proxy(
				promiseResponse(
					"getPlaylist",
					"id=${playlistId.id}"
				)
					.also(::doCancel)
					.promiseSubsonicResponse<SubsonicPlaylistResponse>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: SubsonicPlaylistResponse?): Promise<List<ServiceFile>> = ThreadPools.compute.preparePromise { cs ->
			response?.playlist?.entry?.filter { !it.isDir }?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				ServiceFile(it.id)
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class ConnectionPossiblePromise : Promise.Proxy<Boolean>() {
		init {
			proxy(
				PingViewPromise()
					.also(::doCancel)
					.then(
						{ it?.status == "ok" },
						{ e ->
							when (e) {
								is CancellationException, is InvalidResponseCodeException -> {}

								is NonStandardResponseException -> logger.warn("Non-standard response received.", e)

								is IOException -> logger.error("Unexpected IO exception checking connection", e)

								else -> logger.error(
									"Unexpected error checking connection at URL {}.",
									subsonicConnectionDetails.baseUrl,
									e
								)
							}

							false
						}
					)
			)
		}
	}

	private inner class PingViewPromise : Promise.Proxy<SubsonicResponse>(), PromisedResponse<HttpResponse, SubsonicResponse?> {
		init {
			proxy(
				httpClient
					.promiseResponse(subsonicApiUrl.addPath("ping.view"))
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: HttpResponse): Promise<SubsonicResponse?> {
			if (response.code != 200) throw InvalidResponseCodeException(response.code)

			return response.promiseSubsonicResponse<SubsonicResponse>()
		}
	}

	private class HttpStreamedResponse : ImmediateResponse<HttpResponse?, InputStream>, InputStream() {
		private var savedResponse: HttpResponse? = null
		private lateinit var byteStream: InputStream

		override fun respond(response: HttpResponse?): InputStream {
			savedResponse = response

			byteStream = response
				?.takeIf { it.code != 404 }
				?.run { body }
				?: ByteArrayInputStream(emptyByteArray)

			return this
		}

		override fun read(): Int = byteStream.read()

		override fun read(b: ByteArray, off: Int, len: Int): Int = byteStream.read(b, off, len)

		override fun available(): Int = byteStream.available()

		override fun close() {
			byteStream.close()
			savedResponse?.close()
		}

		override fun toString(): String = byteStream.toString()
	}

	@Keep
	private open class SubsonicResponse(
		val status: String,
		val version: String,
	)

	@Keep
	private class SubsonicNamedItem(
		val id: String,
		val name: String,
		val isDir: Boolean,
	)

	@Keep
	private class SubsonicIndex(
		val artist: List<SubsonicNamedItem>,
	)

	@Keep
	private class SubsonicIndexResponse(
		val index: List<SubsonicIndex>,
	)

	@Keep
	private class SubsonicIndexesResponse(
		val indexes: SubsonicIndexResponse
	)

	@Keep
	private class SubsonicPlaylistDirectoryResponse(
		val playlist: List<SubsonicNamedItem>,
	)

	@Keep
	private class SubsonicPlaylistsResponse(
		val playlists: SubsonicPlaylistDirectoryResponse
	)

	@Keep
	private class SubsonicLastModifiedResponse(
		val lastModified: Long,
	)

	@Keep
	private class SubsonicIndexesLastModifiedResponse(
		val indexes: SubsonicLastModifiedResponse
	)

	@Keep
	private class SubsonicDirectory(
		val child: List<SubsonicNamedItem>
	)

	@Keep
	private class SubsonicDirectoryRoot(
		val directory: SubsonicDirectory
	)

	@Keep
	private class SubsonicLyrics(
		val value: String,
	)

	@Keep
	private class SubsonicLyricsResponse(
		val lyrics: SubsonicLyrics,
	)

	@Keep
	private class SubsonicPlaylist(
		val entry: List<SubsonicNamedItem>,
	)

	@Keep
	private class SubsonicPlaylistResponse(
		val playlist: SubsonicPlaylist,
	)
}
