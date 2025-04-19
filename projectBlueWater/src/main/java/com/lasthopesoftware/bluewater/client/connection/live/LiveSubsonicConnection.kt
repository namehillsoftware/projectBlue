package com.lasthopesoftware.bluewater.client.connection.live

import androidx.annotation.Keep
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.google.gson.JsonParser
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
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
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
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

		private const val musicFormat = "mp3"
		private const val bitrate = "128"

		private const val playlistsItemKey = "playlists"
		private const val artistsItemKey = "artists"

		private val playlistsItem = ItemId(playlistsItemKey)
		private val artistsItem = ItemId(artistsItemKey)
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

	override val dataAccess = this

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

	override fun promiseAudioPlaylistPaths(): Promise<List<String>> = RootPlaylistsPromise()
		.cancelBackThen { items, cancellationSignal ->
			items.mapNotNull {
				if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while getting playlist paths.")
				it.value
			}
		}

	override fun promiseStoredPlaylist(playlistPath: String, playlist: List<ServiceFile>): Promise<*> =
		promiseSubsonicResponse<Response>("createPlaylist", "name=$playlistPath", *playlist.map { "songId=${it.key}" }.toTypedArray())

	override fun promiseIsReadOnly(): Promise<Boolean> =
		promiseSubsonicResponse<UserResponse>("getUser", "username=${subsonicConnectionDetails.userName}")
			.cancelBackThen { response, _ ->
				!(response?.user?.commentRole ?: false)
			}

	override fun promiseServerVersion(): Promise<SemanticVersion?> = ServerVersionPromise()

	override fun promiseFile(serviceFile: ServiceFile): Promise<InputStream> = Promise.Proxy { cp ->
		httpClient
			.promiseResponse(getFileUrl(serviceFile))
			.also(cp::doCancel)
			.then(HttpStreamedResponse())
	}

	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> = promiseResponse("getCoverArt", "id=${serviceFile.key}")
		.cancelBackThen { httpResponse, _ ->
			httpResponse.body.use { it.readBytes() }
		}

	override fun promiseImageBytes(itemId: ItemId): Promise<ByteArray> = promiseResponse("getCoverArt", "id=${itemId.id}")
		.cancelBackThen { httpResponse, _ ->
			httpResponse.body.use { it.readBytes() }
		}

	override fun promiseFileStringList(itemId: ItemId?): Promise<String> = itemId
		?.let(::promiseFiles)
		?.cancelBackEventually(FileStringListUtilities::promiseSerializedFileStringList)
		.keepPromise("")

	override fun promiseFileStringList(playlistId: PlaylistId): Promise<String> = PlaylistFilesPromise(playlistId)
		.cancelBackEventually(FileStringListUtilities::promiseSerializedFileStringList)

	override fun promiseShuffledFileStringList(itemId: ItemId?): Promise<String> = itemId
		?.let(::promiseFiles)
		?.cancelBackEventually(FileStringListUtilities::promiseShuffledSerializedFileStringList)
		.keepPromise("")

	override fun promiseShuffledFileStringList(playlistId: PlaylistId): Promise<String> = PlaylistFilesPromise(playlistId)
		.cancelBackEventually(FileStringListUtilities::promiseShuffledSerializedFileStringList)

	override fun promiseFiles(): Promise<List<ServiceFile>> = Promise(emptyList())

	override fun promiseFiles(query: String): Promise<List<ServiceFile>> = SearchFilesPromise(query)

	override fun promiseFiles(itemId: ItemId): Promise<List<ServiceFile>> = when (itemId) {
		artistsItem, playlistsItem -> Promise(emptyList())
		else -> ItemFilesPromise(itemId)
	}

	override fun promiseFiles(playlistId: PlaylistId): Promise<List<ServiceFile>> = PlaylistFilesPromise(playlistId)

	override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> = promiseResponse(
		"scrobble",
		"id=${serviceFile.key}"
	).cancelBackThen { response, _ ->
		response.use {
			val responseCode = it.code

			if (BuildConfig.DEBUG) {
				logger.debug("rest/scrobble responded with a response code of {}", responseCode)
			}

			if (responseCode < 200 || responseCode >= 300) throw HttpResponseException(responseCode)
		}
	}

	override fun promiseRevision(): Promise<Long?> = RevisionPromise()

	private fun promiseResponse(path: String, vararg params: String): Promise<HttpResponse> {
		val url = subsonicApiUrl.addPath(path).addParams(*params)
		return httpClient.promiseResponse(url)
	}

	private inline fun <reified T> promiseSubsonicResponse(path: String, vararg params: String): Promise<T?> {
		val url = subsonicApiUrl.addPath(path).addParams(*params)
		return httpClient.promiseResponse(url).cancelBackEventually { it.promiseSubsonicResponse() }
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
				if (json.asJsonObject.get("status")?.asString == "failed") {
					throw SubsonicServerException(jsonTranslator.parseJson<ErrorResponse>(json))
				}

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
							.promiseSubsonicResponse<LyricsResponse>()
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
		ImmediateResponse<IndexesLastModifiedResponse?, Long?>
	{
		init {
			proxy(
				promiseResponse("getIndexes")
					.also(::doCancel)
					.promiseSubsonicResponse<IndexesLastModifiedResponse>()
					.also(::doCancel)
					.then(this)
			)
		}

		override fun respond(resolution: IndexesLastModifiedResponse?): Long? = resolution?.indexes?.lastModified
	}

	private inner class RootIndexPromise :
		Promise.Proxy<List<IItem>>(),
		PromisedResponse<IndexesResponse?, List<IItem>>
	{
		init {
			proxy(
				promiseResponse("getIndexes")
					.also(::doCancel)
					.promiseSubsonicResponse<IndexesResponse>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: IndexesResponse?): Promise<List<IItem>> = ThreadPools.compute.preparePromise { cs ->
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
		PromisedResponse<DirectoryRoot?, List<IItem>>
	{
		init {
			proxy(
				promiseResponse(
					"getMusicDirectory",
					"id=${itemId.id}"
				)
					.also(::doCancel)
					.promiseSubsonicResponse<DirectoryRoot>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: DirectoryRoot?): Promise<List<IItem>> = ThreadPools.compute.preparePromise { cs ->
			response?.directory?.child?.filter { it.isDir }?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				Item(it.id, it.name, playlistId = null)
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class RootPlaylistsPromise :
		Promise.Proxy<List<IItem>>(),
		PromisedResponse<PlaylistsResponse?, List<IItem>>
	{
		init {
			proxy(
				promiseResponse("getPlaylists")
					.also(::doCancel)
					.promiseSubsonicResponse<PlaylistsResponse>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: PlaylistsResponse?): Promise<List<IItem>> = ThreadPools.compute.preparePromise { cs ->
			response?.playlists?.playlist?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				Playlist(it.id, it.name)
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class ItemFilesPromise(itemId: ItemId) :
		Promise.Proxy<List<ServiceFile>>(),
		PromisedResponse<DirectoryRoot?, List<ServiceFile>>
	{
		init {
			proxy(
				promiseResponse(
					"getMusicDirectory",
					"id=${itemId.id}"
				)
					.also(::doCancel)
					.promiseSubsonicResponse<DirectoryRoot>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: DirectoryRoot?): Promise<List<ServiceFile>> = ThreadPools.compute.preparePromise { cs ->
			response?.directory?.child?.filter { !it.isDir }?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				ServiceFile(it.id)
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class PlaylistFilesPromise(playlistId: PlaylistId) :
		Promise.Proxy<List<ServiceFile>>(),
		PromisedResponse<PlaylistResponse?, List<ServiceFile>>
	{
		init {
			proxy(
				promiseResponse(
					"getPlaylist",
					"id=${playlistId.id}"
				)
					.also(::doCancel)
					.promiseSubsonicResponse<PlaylistResponse>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: PlaylistResponse?): Promise<List<ServiceFile>> = ThreadPools.compute.preparePromise { cs ->
			response?.playlist?.entry?.filter { !it.isDir }?.map {
				if (cs.isCancelled) throw itemParsingCancelledException()

				ServiceFile(it.id)
			} ?: emptyList()
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class SearchFilesPromise(query: String) :
		Promise.Proxy<List<ServiceFile>>(),
		PromisedResponse<Search2Response?, List<ServiceFile>>
	{
		init {
			proxy(
				promiseResponse(
					"search2.view",
					"query=$query",
					"albumCount=0",
					"songCount=1000"
				)
					.also(::doCancel)
					.promiseSubsonicResponse<Search2Response>()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: Search2Response?): Promise<List<ServiceFile>> = ThreadPools.compute.preparePromise { cs ->
			response?.searchResult2?.song?.filter { !it.isDir }?.map {
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

	private inner class PingViewPromise : Promise.Proxy<Response>(), PromisedResponse<HttpResponse, Response?> {
		init {
			proxy(
				httpClient
					.promiseResponse(subsonicApiUrl.addPath("ping.view"))
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: HttpResponse): Promise<Response?> {
			if (response.code != 200) throw InvalidResponseCodeException(response.code)

			return response.promiseSubsonicResponse<Response>()
		}
	}

	private inner class ServerVersionPromise : Promise.Proxy<SemanticVersion?>(), ImmediateResponse<Throwable, SemanticVersion?> {
		init {
			proxy(
				cachedVersion
					.also(::doCancel)
					.then(forward(), this)
			)
		}

		override fun respond(resolution: Throwable?): SemanticVersion? = null
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
	private open class Response(
		val status: String,
		val version: String,
	)

	@Keep
	private class NamedItem(
		val id: String,
		val name: String,
		val isDir: Boolean,
	)

	@Keep
	private class Index(
		val artist: List<NamedItem>,
	)

	@Keep
	private class IndexResponse(
		val index: List<Index>,
	)

	@Keep
	private class IndexesResponse(
		val indexes: IndexResponse
	)

	@Keep
	private class PlaylistDirectoryResponse(
		val playlist: List<NamedItem>,
	)

	@Keep
	private class PlaylistsResponse(
		val playlists: PlaylistDirectoryResponse
	)

	@Keep
	private class LastModifiedResponse(
		val lastModified: Long,
	)

	@Keep
	private class IndexesLastModifiedResponse(
		val indexes: LastModifiedResponse
	)

	@Keep
	private class Directory(
		val child: List<NamedItem>
	)

	@Keep
	private class DirectoryRoot(
		val directory: Directory
	)

	@Keep
	private class Lyrics(
		val value: String,
	)

	@Keep
	private class LyricsResponse(
		val lyrics: Lyrics,
	)

	@Keep
	private class SubsonicPlaylist(
		val entry: List<NamedItem>,
	)

	@Keep
	private class PlaylistResponse(
		val playlist: SubsonicPlaylist,
	)

	@Keep
	@Suppress("unused")
	private class ErrorResponse(
		val code: Int,
		val message: String,
	)

	@Keep
	private class UserDetailsResponse(
		val commentRole: Boolean,
	)

	@Keep
	private class UserResponse(
		val user: UserDetailsResponse,
	)

	@Keep
	private class Search2ResponseItems(
		val song: List<NamedItem>,
	)

	@Keep
	private class Search2Response(
		val searchResult2: Search2ResponseItems,
	)

	@Suppress("unused")
	private class SubsonicServerException(val error: ErrorResponse?) : IOException("Server returned 'failed'.")
}
