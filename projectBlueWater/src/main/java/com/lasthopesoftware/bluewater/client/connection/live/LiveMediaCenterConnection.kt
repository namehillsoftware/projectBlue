package com.lasthopesoftware.bluewater.client.connection.live

import android.os.Build
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.durationInMs
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.requests.bodyString
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withMcApi
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.StandardResponse.Companion.toStandardResponse
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.NonStandardResponseException
import com.lasthopesoftware.resources.io.promiseStandardResponse
import com.lasthopesoftware.resources.io.promiseStringBody
import com.lasthopesoftware.resources.io.promiseXmlDocument
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

class LiveMediaCenterConnection(
	private val mediaCenterConnectionDetails: MediaCenterConnectionDetails,
	private val httpPromiseClients: ProvideHttpPromiseClients,
	private val okHttpClients: ProvideOkHttpClients,
) : LiveServerConnection, RemoteLibraryAccess
{
	companion object {
		private val logger by lazyLogger<LiveMediaCenterConnection>()
		private const val browseFilesPath = "Browse/Files"
		private const val playlistFilesPath = "Playlist/Files"
		private const val searchFilesPath = "Files/Search"
		private const val browseLibraryParameter = "Browse/Children"
		private const val imageFormat = "jpg"

		private val editableFilePropertyDefinitions by lazy { EditableFilePropertyDefinition.entries.toSet().toPromise() }
	}

	private val mcApiUrl by lazy { mediaCenterConnectionDetails.baseUrl.withMcApi() }

	private val httpClient by lazy { httpPromiseClients.getServerClient(mediaCenterConnectionDetails) }

	private val cachedServerVersionPromise by RetryOnRejectionLazyPromise {
		Promise.Proxy { cp ->
			promiseResponse("Alive")
				.also(cp::doCancel)
				.promiseStandardResponse()
				.also(cp::doCancel)
				.then { standardRequest ->
					standardRequest.items["ProgramVersion"]
						?.let { semVerString ->
							val semVerParts = semVerString.split(".")
							var major = 0
							var minor = 0
							var patch = 0
							if (semVerParts.isNotEmpty()) major = semVerParts[0].toInt()
							if (semVerParts.size > 1) minor = semVerParts[1].toInt()
							if (semVerParts.size > 2) patch = semVerParts[2].toInt()
							SemanticVersion(major, minor, patch)
						}
				}
		}
	}

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(mediaCenterConnectionDetails.baseUrl, key)

	override fun getFileUrl(serviceFile: ServiceFile): URL =
			mcApiUrl
				.addPath("File/GetFile")
				.addParams(
					"File=${serviceFile.key}",
					"Quality=Medium",
					"Conversion=Android",
					"Playback=0",
					"AndroidVersion=${Build.VERSION.RELEASE}"
				)

	override val dataSourceFactory by lazy {
		OkHttpDataSource.Factory(
			okHttpClients
				.getOkHttpClient(mediaCenterConnectionDetails)
				.newBuilder()
				.readTimeout(45, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
				.build())
	}

	override val dataAccess: RemoteLibraryAccess
		get() = this

	override fun promiseIsConnectionPossible(): Promise<Boolean> = ConnectionPossiblePromise()

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		FilePropertiesPromise(serviceFile)

	override fun promiseFilePropertyUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		promiseResponse("File/SetInfo", "File=${serviceFile.key}", "Field=$property", "Value=$value", "formatted=" + if (isFormatted) "1" else "0")
			.cancelBackThen { response, cs ->
				if (cs.isCancelled && BuildConfig.DEBUG) logger.debug("File property update cancelled.")

				response.use {
					logger.info("api/v1/File/SetInfo responded with a response code of {}.", it.code)
				}
			}

	override fun promiseItems(itemId: KeyedIdentifier?): Promise<List<IItem>> = when (itemId) {
		is ItemId -> ItemFilePromise(itemId)
		null -> ItemFilePromise(null)
		else -> Promise(emptyList())
	}

	override fun promiseAudioPlaylistPaths(): Promise<List<String>> = Promise.Proxy { cp ->
		promiseResponse("Playlists/List", "IncludeMediaTypes=1")
			.also(cp::doCancel)
			.promiseStringBody()
			.also(cp::doCancel)
			.promiseXmlDocument()
			.also(cp::doCancel)
			.then { xml ->
				xml
					.getElementsByTag("Item")
					.mapNotNull { itemXml ->
						itemXml
							.takeIf {
								it.getElementsByTag("Field")
									.any { el -> el.attr("Name") == "MediaTypes" && el.ownText() == "Audio" }
							}
							?.getElementsByTag("Field")
							?.firstOrNull { el -> el.attr("Name") == "Path" }
							?.ownText()
					}
			}
	}

	override fun promiseStoredPlaylist(playlistPath: String, playlist: List<ServiceFile>): Promise<*> = Promise.Proxy { cp ->
		promiseResponse("Playlists/Add", "Type=Playlist", "Path=$playlistPath", "CreateMode=Overwrite")
			.also(cp::doCancel)
			.promiseStandardResponse()
			.also(cp::doCancel)
			.then { it -> it.items["PlaylistID"] }
			.eventually {
				it?.let { playlistId ->
					ThreadPools.compute
						.preparePromise { playlist.joinToString(",") { sf -> sf.key } }
						.also(cp::doCancel)
						.eventually { keys ->
							promiseResponse(
								"Playlist/AddFiles",
								"PlaylistType=ID",
								"Playlist=$playlistId",
								"Keys=$keys",
							)
						}
				}.keepPromise()
			}
	}

	override fun promiseIsReadOnly(): Promise<Boolean> = Promise.Proxy { cp ->
		promiseResponse("Authenticate")
			.also(cp::doCancel)
			.promiseStandardResponse()
			.also(cp::doCancel)
			.then { sr ->
				sr.items["ReadOnly"]?.toInt()?.let { ro -> ro != 0 } ?: false
			}
	}

	override fun promiseServerVersion(): Promise<SemanticVersion?> = cachedServerVersionPromise

	override fun promiseFile(serviceFile: ServiceFile): Promise<InputStream> =
		Promise.Proxy { cp ->
			httpClient
				.promiseResponse(getFileUrl(serviceFile))
				.also(cp::doCancel)
				.then(HttpStreamedResponse())
		}

	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> =
		Promise.Proxy { cp ->
			promiseResponse(
				"File/GetImage",
				"File=${serviceFile.key}",
				"Type=Full",
				"Pad=1",
				"Format=$imageFormat",
				"FillTransparency=ffffff"
			).also(cp::doCancel).then { response ->
				response?.use {
					if (cp.isCancelled) throw CancellationException("Cancelled while retrieving image")
					else when (response.code) {
						200 -> response.body.use { it.readBytes() }
						else -> emptyByteArray
					}
				} ?: emptyByteArray
			}
		}

	override fun promiseImageBytes(itemId: ItemId): Promise<ByteArray> =
		promiseResponse(
			"Browse/Image",
			"ID=${itemId.id}",
			"Type=Full",
			"Pad=1",
			"Format=$imageFormat",
			"FillTransparency=ffffff",
			"UseStackedImages=0",
			"Version=2",
		).cancelBackThen { response, cp ->
			response.use {
				if (cp.isCancelled) throw CancellationException("Cancelled while retrieving image")
				else when (response.code) {
					200 -> response.body.use { it.readBytes() }
					else -> emptyByteArray
				}
			}
		}

	override fun promiseEditableFilePropertyDefinitions(): Promise<Set<EditableFilePropertyDefinition>> = editableFilePropertyDefinitions

	override fun promiseFileStringList(itemId: ItemId?): Promise<String> =
		itemId
			?.run {
				promiseFileStringList(FileListParameters.Options.None, browseFilesPath, "ID=$id", "Version=2")
			}
			?: promiseFileStringList(FileListParameters.Options.None, browseFilesPath, "Version=2")

	override fun promiseFileStringList(playlistId: PlaylistId): Promise<String> = "".toPromise()

	override fun promiseShuffledFileStringList(itemId: ItemId?): Promise<String> =
		itemId
			?.run {
				promiseFileStringList(FileListParameters.Options.Shuffled, browseFilesPath, "ID=$id", "Version=2")
			}
			?: promiseFileStringList(FileListParameters.Options.Shuffled, browseFilesPath, "Version=2")

	override fun promiseShuffledFileStringList(playlistId: PlaylistId): Promise<String> = "".toPromise()

	override fun promiseFiles(): Promise<List<ServiceFile>> =
		promiseFilesAtPath(browseFilesPath, "Version=2")

	override fun promiseFiles(query: String): Promise<List<ServiceFile>> =
		promiseFilesAtPath(searchFilesPath, "Query=$query")

	override fun promiseFiles(itemId: ItemId): Promise<List<ServiceFile>> =
		promiseFilesAtPath(browseFilesPath, "ID=${itemId.id}", "Version=2")

	override fun promiseFiles(playlistId: PlaylistId): Promise<List<ServiceFile>> =
		promiseFilesAtPath(playlistFilesPath, "Playlist=${playlistId.id}")

	override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> = Promise.Proxy { cp ->
		promiseServerVersion()
			.also(cp::doCancel)
			.eventually { v ->
				if (v != null && v.major >= 22) promiseResponse("File/Played", "File=" + serviceFile.key, "FileType=Key")
					.also(cp::doCancel)
					.then { response ->
						response.use {
							val responseCode = it.code
							logger.debug("api/v1/File/Played responded with a response code of {}", responseCode)
							if (responseCode < 200 || responseCode >= 300) throw HttpResponseException(responseCode)
						}
					}
				else FilePropertiesPlayStatsUpdatePromise(serviceFile)
			}
	}

	override fun promiseRevision(): Promise<Long?> = Promise.Proxy { cp ->
		promiseResponse("Library/GetRevision")
			.also(cp::doCancel)
			.promiseStandardResponse()
			.also(cp::doCancel)
			.then { standardRequest ->
				standardRequest.items["Sync"]
					?.takeIf { revisionValue -> revisionValue.isNotEmpty() }
					?.toLong()
			}
	}

	private fun promiseFilesAtPath(path: String, vararg params: String): Promise<List<ServiceFile>> =
		Promise.Proxy { cp ->
			promiseFileStringList(FileListParameters.Options.None, path, *params)
				.also(cp::doCancel)
				.eventually(FileResponses)
				.also(cp::doCancel)
				.then(FileResponses)
		}

	private fun promiseFileStringList(option: FileListParameters.Options, path: String, vararg params: String): Promise<String> =
		Promise.Proxy { cp ->
			promiseResponse(
				path,
				*FileListParameters.Helpers.processParams(
					option,
					*params
				)
			).also(cp::doCancel).promiseStringBody()
		}

	private fun promiseResponse(path: String, vararg params: String): Promise<HttpResponse> {
		val url = mcApiUrl.addPath(path).addParams(*params)
		return httpClient.promiseResponse(url)
	}

	private inner class FilePropertiesPromise(private val serviceFile: ServiceFile) :
		Promise<Map<String, String>>(),
		PromisedResponse<HttpResponse, Unit>,
		CancellableMessageWriter<Unit>,
		ImmediateResponse<Throwable, Unit>
	{
		private val cancellationProxy = CancellationProxy()
		private lateinit var responseString: String

		init {
			awaitCancellation(cancellationProxy)
			val filePropertiesResponse = promiseResponse("File/GetInfo", "File=" + serviceFile.key)
			val promisedProperties = filePropertiesResponse.eventually(this)

			// Handle cancellation errors directly in stack so that they don't become unhandled
			promisedProperties.excuse(this)

			cancellationProxy.doCancel(filePropertiesResponse)
		}

		override fun promiseResponse(response: HttpResponse): Promise<Unit> {
			responseString = response.use {
				if (cancellationProxy.isCancelled) {
					reject(filePropertiesCancellationException(serviceFile))
					return Unit.toPromise()
				}

				it.bodyString
			}

			return ThreadPools.compute.preparePromise(this)
		}

		override fun prepareMessage(cancellationSignal: CancellationSignal) {
			if (cancellationSignal.isCancelled) {
				reject(filePropertiesCancellationException(serviceFile))
				return
			}

			resolve(
				responseString
					.let(Jsoup::parse)
					.let { xml ->
						xml
							.getElementsByTag("item")
							.firstOrNull()
							?.children()
							?.associateTo(HashMap()) { el ->
								if (cancellationSignal.isCancelled) {
									reject(filePropertiesCancellationException(serviceFile))
									return
								}

								Pair(el.attr("Name"), el.wholeOwnText())
							}
							?: emptyMap()
					}
			)
		}

		override fun respond(rejection: Throwable) {
			if (cancellationProxy.isCancelled && rejection is IOException && rejection.isOkHttpCanceled()) {
				reject(filePropertiesCancellationException(serviceFile))
			} else {
				reject(rejection)
			}
		}

		private fun filePropertiesCancellationException(serviceFile: ServiceFile) =
			CancellationException("Getting file properties cancelled for $serviceFile.")
	}

	private inner class ItemFilePromise(
		itemId: ItemId?,
	) : Promise.Proxy<List<IItem>>(), PromisedResponse<Document, List<IItem>> {
		init {
			val promisedResponse = itemId
				?.run {
					promiseResponse(
						browseLibraryParameter,
						"ID=$id",
						"Version=2",
						"ErrorOnMissing=1"
					)
				}
				?: promiseResponse(browseLibraryParameter, "Version=2", "ErrorOnMissing=1")

			proxy(
				promisedResponse
					.also(::doCancel)
					.promiseStringBody()
					.also(::doCancel)
					.promiseXmlDocument()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(document: Document): Promise<List<IItem>> = ThreadPools.compute.preparePromise { cs ->
			val body = document.getElementsByTag("Response").firstOrNull()
				?: throw IOException("Response tag not found")

			val status = body.attr("Status")
			if (status.equals("Failure", ignoreCase = true))
				throw IOException("Server returned 'Failure'.")

			body
				.getElementsByTag("Item")
				.map { el ->
					if (cs.isCancelled) throw itemParsingCancelledException()
					Item(el.wholeOwnText(), el.attr("Name"))
				}
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class ConnectionPossiblePromise : Promise<Boolean>() {
		init {
			val cancellationProxy = CancellationProxy()
			awaitCancellation(cancellationProxy)

			promiseResponse("Alive")
				.also(cancellationProxy::doCancel)
				.then(
					{ it, cp -> resolve(testResponse(it, cp)) },
					{ e, _ ->
						logger.error("Error checking connection at URL {}.", mediaCenterConnectionDetails.baseUrl, e)
						resolve(false)
					}
				)
				.also(cancellationProxy::doCancel)
		}

		private fun testResponse(response: HttpResponse, cancellationSignal: CancellationSignal): Boolean {
			response.use { r ->
				if (cancellationSignal.isCancelled) return false

				try {
					return r.bodyString.let { Jsoup.parse(it, Parser.xmlParser()) }.toStandardResponse().isStatusOk
				} catch (e: NonStandardResponseException) {
					logger.warn("Non standard response received.", e)
				} catch (e: IOException) {
					logger.error("Error closing connection, device failure?", e)
				} catch (e: IllegalArgumentException) {
					logger.warn("Illegal argument passed in", e)
				} catch (t: Throwable) {
					logger.error("Unexpected error parsing response.", t)
				}
			}
			return false
		}
	}

	private inner class FilePropertiesPlayStatsUpdatePromise(private val serviceFile: ServiceFile) :
		Promise.Proxy<Unit>(), PromisedResponse<Map<String, String>, Unit>
	{
		init {
			proxy(
				FilePropertiesPromise(serviceFile)
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(fileProperties: Map<String, String>): Promise<Unit> {
			try {
				val lastPlayedServer = fileProperties[NormalizedFileProperties.LastPlayed]
				val duration = fileProperties.durationInMs ?: 0
				val currentTime = System.currentTimeMillis()
				if (lastPlayedServer != null && currentTime - duration <= lastPlayedServer.toLong() * 1000) return Unit.toPromise()

				val numberPlaysString = fileProperties[NormalizedFileProperties.NumberPlays]
				val numberPlays = (numberPlaysString?.toIntOrNull() ?: 0) + 1
				val numberPlaysUpdate = promiseFilePropertyUpdate(
					serviceFile,
					NormalizedFileProperties.NumberPlays,
					numberPlays.toString(),
					false
				)

				val newLastPlayed = (currentTime / 1000).toString()
				val lastPlayedUpdate = promiseFilePropertyUpdate(
					serviceFile,
					NormalizedFileProperties.LastPlayed,
					newLastPlayed,
					false
				)

				return Promise.whenAll(numberPlaysUpdate, lastPlayedUpdate).unitResponse()
			} catch (ne: NumberFormatException) {
				logger.error(ne.toString(), ne)
				return Unit.toPromise()
			}
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

	private object FileResponses : PromisedResponse<String, Collection<ServiceFile>>, ImmediateResponse<Collection<ServiceFile>, List<ServiceFile>> {
		private val emptyListPromise by lazy { Promise<Collection<ServiceFile>>(emptyList()) }

		override fun promiseResponse(stringList: String?): Promise<Collection<ServiceFile>> {
			return stringList?.let(FileStringListUtilities::promiseParsedFileStringList) ?: emptyListPromise
		}

		override fun respond(serviceFiles: Collection<ServiceFile>): List<ServiceFile> {
			return if (serviceFiles is List<*>) serviceFiles as List<ServiceFile> else serviceFiles.toList()
		}
	}
}
