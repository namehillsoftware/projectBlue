package com.lasthopesoftware.bluewater.client.connection.live

import androidx.annotation.Keep
import androidx.media3.datasource.DataSource
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LookupFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.dateTimeProperties
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideServerHttpDataSource
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.ForwardedResponse.Companion.forward
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.instead
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.closables.thenUse
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.InvalidResponseCodeException
import com.lasthopesoftware.resources.io.NonStandardResponseException
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.lasthopesoftware.resources.io.promiseJson
import com.lasthopesoftware.resources.io.promiseStringBody
import com.lasthopesoftware.resources.strings.GetStringResources
import com.lasthopesoftware.resources.strings.TranslateJson
import com.lasthopesoftware.resources.strings.parseJson
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.joda.time.DateTime
import java.io.IOException
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

class LiveSubsonicConnection(
	private val subsonicConnectionDetails: SubsonicConnectionDetails,
	private val httpPromiseClients: ProvideHttpPromiseServerClients<SubsonicConnectionDetails>,
	private val serverHttpDataSource: ProvideServerHttpDataSource<SubsonicConnectionDetails>,
	private val jsonTranslator: TranslateJson,
	private val stringResources: GetStringResources,
) : LiveServerConnection, RemoteLibraryAccess {
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
		const val track = "track"
		const val discNumber = "discNumber"
		const val size = "size"
		const val title = "title"
		const val id = "id"
		const val artist = "artist"
		const val replayGain = "replayGain"
		const val trackGain = "trackGain"
		const val peakGain = "trackPeak"
		const val userRating = "userRating"
		const val playCount = "playCount"
		const val played = "played"
		const val duration = "duration"
		const val genre = "genre"
	}

	private val promisedRootItem by lazy {
		Promise(
			listOf<IItem>(
				Item(artistsItemKey, stringResources.artists, null),
				Item(playlistsItemKey, stringResources.playlists, null),
			)
		)
	}

	private val subsonicApiUrl by lazy { subsonicConnectionDetails.baseUrl.withSubsonicApi().addParams("f=json") }

	private val httpClient by RetryOnRejectionLazyPromise {
		httpPromiseClients.promiseServerClient(
			subsonicConnectionDetails
		)
	}

	private val cachedVersion by RetryOnRejectionLazyPromise {
		pingViewPromise().cancelBackThen { r, _ ->
			r?.version?.split(".")?.let {
				SemanticVersion(it[0].toInt(), it[1].toInt(), it[2].toInt())
			}
		}
	}

	private val promisedDataSourceFactory by RetryOnRejectionLazyPromise {
		serverHttpDataSource.promiseDataSourceFactory(subsonicConnectionDetails)
	}

	private val localRevision = AtomicInteger()

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(subsonicConnectionDetails.baseUrl, key)

	override fun getFileUrl(serviceFile: ServiceFile): URL =
		subsonicApiUrl
			.addPath("stream")
			.addParams(
				"id=${serviceFile.key}",
				"format=$musicFormat",
				"maxBitRate=$bitrate",
			)

	override fun promiseDataSourceFactory(): Promise<DataSource.Factory> = promisedDataSourceFactory

	override val dataAccess = this

	override fun promiseIsConnectionPossible(): Promise<Boolean> = ConnectionPossiblePromise()

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<LookupFileProperties> =
		FilePropertiesPromise(serviceFile)

	override fun promiseFilePropertyUpdate(
		serviceFile: ServiceFile,
		property: String,
		value: String,
		isFormatted: Boolean
	): Promise<Unit> {
		if (property != NormalizedFileProperties.Rating) throw IllegalArgumentException(property)
		return promiseSubsonicResponse<Response>("setRating", "id=${serviceFile.key}", "rating=$value")
			.cancelBackThen { _, _ ->
				localRevision.incrementAndGet()
			}
	}

	override fun promiseItems(itemId: KeyedIdentifier?): Promise<List<IItem>> = when (itemId) {
		null -> promisedRootItem
		artistsItem -> RootIndexPromise()
		playlistsItem -> RootPlaylistsPromise()
		is ItemId -> ItemPromise(itemId)
		else -> Promise(emptyList())
	}

	override fun promiseAudioPlaylistPaths(): Promise<List<String>> = RootPlaylistsPromise()
		.cancelBackThen { items, cancellationSignal ->
			items.mapNotNull {
				if (cancellationSignal.isCancelled) throw CancellationException("Cancelled while getting playlist paths.")
				it.value
			}
		}

	override fun promiseStoredPlaylist(playlistPath: String, playlist: List<ServiceFile>): Promise<*> =
		RootPlaylistsPromise()
			.cancelBackEventually { playlists ->
				val existingPlaylistId = playlists.firstOrNull { it.value == playlistPath }?.itemId
				promiseSubsonicResponse<Response>(
					"createPlaylist",
					existingPlaylistId?.id?.let { "playlistId=$it" } ?: "name=$playlistPath",
					*playlist.map { "songId=${it.key}" }.toTypedArray()
				)
			}

	override fun promiseIsReadOnly(): Promise<Boolean> =
		promiseSubsonicResponse<UserResponse>("getUser", "username=${subsonicConnectionDetails.userName}")
			.cancelBackThen { response, _ ->
				response?.user?.run { !adminRole && !commentRole } ?: true
			}

	override fun promiseServerVersion(): Promise<SemanticVersion?> = ServerVersionPromise()

	override fun promiseFile(serviceFile: ServiceFile): Promise<PromisingReadableStream> =
		httpClient
			.eventually { it.promiseResponse(getFileUrl(serviceFile)) }
			.cancelBackThen(HttpStreamedResponse())

	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> =
		promiseResponse("getCoverArt", "id=${serviceFile.key}")
			.cancelBackEventually { httpResponse ->
				httpResponse.body.eventuallyUse { it.promiseReadAllBytes() }
			}

	override fun promiseImageBytes(itemId: ItemId): Promise<ByteArray> =
		promiseResponse("getCoverArt", "id=${itemId.id}")
			.cancelBackEventually { httpResponse ->
				httpResponse.body.eventuallyUse { it.promiseReadAllBytes() }
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

	override fun promiseShuffledFileStringList(playlistId: PlaylistId): Promise<String> =
		PlaylistFilesPromise(playlistId)
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
	).cancelBackEventually { response ->
		response.thenUse {
			val responseCode = it.code

			if (BuildConfig.DEBUG) {
				logger.debug("rest/scrobble responded with a response code of {}", responseCode)
			}

			if (responseCode !in 200..<300) throw HttpResponseException(responseCode)

			localRevision.incrementAndGet()
		}
	}

	override fun promiseRevision(): Promise<Long?> = RevisionPromise()

	private fun pingViewPromise() = promiseSubsonicResponse<Response>("ping.view")

	private fun promiseResponse(path: String, vararg params: String): Promise<HttpResponse> {
		val url = subsonicApiUrl.addPath(path).addParams(*params)
		return httpClient.eventually { it.promiseResponse(url) }
	}

	private inline fun <reified T> promiseSubsonicResponse(path: String, vararg params: String): Promise<T?> {
		return object : Promise.Proxy<T?>(), PromisedResponse<HttpResponse, HttpResponse> {
			init {
				proxy(
					promiseResponse(path, *params)
						.also(::doCancel)
						.eventually(this)
						.promiseStringBody()
						.also(::doCancel)
						.promiseSubsonicResponse()
				)
			}

			override fun promiseResponse(r: HttpResponse): Promise<HttpResponse> = when {
				isCancelled -> r.promiseClose().instead(parsingCancelledException())
				r.code !in 200..<300 -> r.promiseClose().instead(HttpResponseException(r.code))
				else  -> r.toPromise()
			}
		}
	}

	private inline fun <reified T> Promise<String>.promiseSubsonicResponse(): Promise<T?> =
		promiseJson()
			.cancelBackEventually { json ->
				ThreadPools.compute.preparePromise { cs ->
					val response = json?.asJsonObject?.get("subsonic-response") ?: throw NonStandardResponseException()
					if (response.asJsonObject.get("status")?.asString == "failed") {
						throw SubsonicServerException(jsonTranslator.parseJson<ErrorResponse>(json))
					}

					if (!cs.isCancelled) jsonTranslator.parseJson<T>(response)
					else throw parsingCancelledException()
				}
			}

	private fun parsingCancelledException() = CancellationException("Parsing cancelled.")

	private class SubsonicFilePropertiesLookup(
		private val filePropertiesMap: MutableMap<String, String>
	) : FilePropertiesLookup() {

		companion object {
			private val normalizedToKnown by lazy {
				mapOf(
					NormalizedFileProperties.Artist to KnownFileProperties.artist,
					NormalizedFileProperties.Key to KnownFileProperties.id,
					NormalizedFileProperties.Name to KnownFileProperties.title,
					NormalizedFileProperties.VolumeLevelReplayGain to KnownFileProperties.trackGain,
					NormalizedFileProperties.PeakLevel to KnownFileProperties.peakGain,
					NormalizedFileProperties.Rating to KnownFileProperties.userRating,
					NormalizedFileProperties.NumberPlays to KnownFileProperties.playCount,
					NormalizedFileProperties.LastPlayed to KnownFileProperties.played,
					NormalizedFileProperties.Duration to KnownFileProperties.duration,
					NormalizedFileProperties.Genre to KnownFileProperties.genre,
					NormalizedFileProperties.FileSize to KnownFileProperties.size,
					NormalizedFileProperties.Track to KnownFileProperties.track,
					NormalizedFileProperties.DiscNumber to KnownFileProperties.discNumber,
				)
			}

			private val knownToNormalized by lazy {
				normalizedToKnown.entries.associate { (k, v) -> v to k }
			}
		}

		override val availableProperties by lazy {
			filePropertiesMap.keys.map { knownToNormalized[it] ?: it }.toSet()
		}

		override fun getValue(name: String): String? {
			var property = filePropertiesMap[normalizedToKnown[name] ?: name] ?: filePropertiesMap[name.lowercase()]
			if (property != null && dateTimeProperties.contains(name)) {
				property = (DateTime.parse(property).millis / 1000.0).toString()
			}

			return property
		}

		override fun isEditable(name: String): Boolean = name == NormalizedFileProperties.Rating

		override fun updateValue(name: String, value: String) {
			filePropertiesMap[name] = value
		}
	}

	private inner class FilePropertiesPromise(private val serviceFile: ServiceFile) :
		Promise.Proxy<LookupFileProperties>(),
		PromisedResponse<HashMap<String, String>, LookupFileProperties>
	{
		init {
			proxy(
				promiseResponse("getSong", "id=${serviceFile.key}")
					.also(::doCancel)
					.promiseStringBody()
					.promiseJson()
					.also(::doCancel)
					.eventually { json ->
						ThreadPools.compute.preparePromise { cs ->
							val subsonicResponse = json?.asJsonObject?.get("subsonic-response") ?: throw NonStandardResponseException()

							if (cs.isCancelled) throw filePropertiesCancellationException(serviceFile)
							val song = subsonicResponse.asJsonObject?.get("song")

							if (cs.isCancelled) throw filePropertiesCancellationException(serviceFile)
							val elements = song?.asJsonObject?.asMap() ?: emptyMap()

							val returnElements = elements
								.mapNotNull { (k, v) ->
									if (cs.isCancelled) throw filePropertiesCancellationException(serviceFile)
									if (v.isJsonPrimitive) k to v.asString else null
								}
								.toMap(HashMap<String, String>(elements.size + 4))

							if (cs.isCancelled) throw filePropertiesCancellationException(serviceFile)

							elements[KnownFileProperties.replayGain]?.asJsonObject
								?.asMap()
								?.mapNotNull { (k, v) ->
									if (cs.isCancelled) throw filePropertiesCancellationException(serviceFile)
									if (v.isJsonPrimitive) k to v.asString else null
								}
								?.let(returnElements::putAll)

							returnElements
						}
					}
					.also(::doCancel)
					.eventually(this, ::handleException)
			)
		}

		override fun promiseResponse(props: HashMap<String, String>): Promise<LookupFileProperties> =
			promiseSubsonicResponse<LyricsResponse>(
				"getLyrics",
				"artist=${props[KnownFileProperties.artist]}",
				"title=${props[KnownFileProperties.title]}"
			)
				.also(::doCancel)
				.then { l ->
					props[NormalizedFileProperties.Lyrics] = l?.lyrics?.value ?: ""
					SubsonicFilePropertiesLookup(props)
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
				promiseSubsonicResponse<IndexesLastModifiedResponse>("getIndexes")
					.also(::doCancel)
					.then(this)
			)
		}

		override fun respond(resolution: IndexesLastModifiedResponse?): Long =
			resolution?.indexes?.lastModified?.let { it + localRevision.get() } ?: localRevision.get().toLong()
	}

	private inner class RootIndexPromise :
		Promise.Proxy<List<IItem>>(),
		PromisedResponse<IndexesResponse?, List<IItem>> {
		init {
			proxy(
				promiseSubsonicResponse<IndexesResponse>("getIndexes")
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(response: IndexesResponse?): Promise<List<IItem>> = response?.indexes?.index?.run {
				ThreadPools.compute.preparePromise { cs ->
					flatMap {
						it.artist.map { artist ->
							if (cs.isCancelled) throw itemParsingCancelledException()

							Item(artist.id, artist.name, playlistId = null) as IItem
						}
					}
				}
			}.keepPromise(::emptyList)

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class ItemPromise(itemId: ItemId) :
		Promise.Proxy<List<IItem>>(),
		PromisedResponse<DirectoryRoot?, List<IItem>>
	{
		init {
			proxy(
				promiseSubsonicResponse<DirectoryRoot>(
					"getMusicDirectory",
					"id=${itemId.id}"
				)
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
				promiseSubsonicResponse<PlaylistsResponse>("getPlaylists")
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
				promiseSubsonicResponse<DirectoryRoot>("getMusicDirectory", "id=${itemId.id}")
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
				promiseSubsonicResponse<PlaylistResponse>("getPlaylist", "id=${playlistId.id}")
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
				promiseSubsonicResponse<Search2Response>(
					"search2.view",
					"query=$query",
					"albumCount=0",
					"songCount=1000")
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
				pingViewPromise()
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
		val adminRole: Boolean,
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
