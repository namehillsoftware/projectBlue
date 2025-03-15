package com.lasthopesoftware.bluewater.client.access

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.FileResponses
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.bodyString
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.promiseStandardResponse
import com.lasthopesoftware.resources.io.promiseStreamedResponse
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
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException

class JRiverLibraryAccess(private val connectionProvider: ProvideConnections) : RemoteLibraryAccess {

	companion object {
		private const val browseFilesPath = "Browse/Files"
		private const val playlistFilesPath = "Playlist/Files"
		private const val searchFilesPath = "Files/Search"
		private const val browseLibraryParameter = "Browse/Children"
		private const val imageFormat = "jpg"

		private val logger by lazyLogger<JRiverLibraryAccess>()
	}

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> =
		FilePropertiesPromise(connectionProvider, serviceFile)

	override fun promiseFilePropertyUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		connectionProvider.promiseResponse("File/SetInfo", "File=${serviceFile.key}", "Field=$property", "Value=$value", "formatted=" + if (isFormatted) "1" else "0")
			.then { response ->
				response.use {
					logger.info("api/v1/File/SetInfo responded with a response code of {}.", it.code)
				}
			}

	override fun promiseItems(itemId: ItemId?): Promise<List<Item>> = ItemFilePromise(connectionProvider, itemId)

	override fun promiseAudioPlaylistPaths(): Promise<List<String>> = Promise.Proxy { cp ->
		connectionProvider.promiseResponse("Playlists/List", "IncludeMediaTypes=1")
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

	override fun promiseStoredPlaylist(playlistPath: String, playlist: List<ServiceFile>): Promise<*> =
		connectionProvider.promiseResponse("Playlists/Add", "Type=Playlist", "Path=$playlistPath", "CreateMode=Overwrite")
			.promiseStandardResponse()
			.then { it -> it.items["PlaylistID"] }
			.eventually {
				it?.let { playlistId ->
					ThreadPools.compute
						.preparePromise { playlist.map { sf -> sf.key }.joinToString(",") }
						.eventually { keys ->
							connectionProvider.promiseResponse(
								"Playlist/AddFiles",
								"PlaylistType=ID",
								"Playlist=$playlistId",
								"Keys=$keys",
							)
						}
				}.keepPromise()
			}

	override fun promiseIsReadOnly(): Promise<Boolean> =
		connectionProvider.promiseResponse("Authenticate")
			.promiseStandardResponse()
			.then { sr ->
				sr.items["ReadOnly"]?.toInt()?.let { ro -> ro != 0 } ?: false
			}

	override fun promiseServerVersion(): Promise<SemanticVersion?> = connectionProvider
		.promiseResponse("Alive")
		.promiseStandardResponse()
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

	override fun promiseFile(serviceFile: ServiceFile): Promise<InputStream> =
		Promise.Proxy { cp ->
			connectionProvider
				.promiseResponse("File/GetFile", *ServiceFileUriQueryParamsProvider.getServiceFileUriQueryParams(serviceFile))
				.also(cp::doCancel)
				.promiseStreamedResponse()
		}

	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> =
		Promise.Proxy { cp ->
			connectionProvider.promiseResponse(
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
			connectionProvider.promiseResponse(
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

	override fun promiseFileStringList(itemId: ItemId?): Promise<String> =
		itemId
			?.run {
				promiseFileStringList(FileListParameters.Options.None, browseFilesPath, "ID=$id", "Version=2")
			}
			?: promiseFileStringList(FileListParameters.Options.None, browseFilesPath, "Version=2")

	override fun promiseShuffledFileStringList(itemId: ItemId?): Promise<String> =
		itemId
			?.run {
				promiseFileStringList(FileListParameters.Options.Shuffled, browseFilesPath, "ID=$id", "Version=2")
			}
			?: promiseFileStringList(FileListParameters.Options.Shuffled, browseFilesPath, "Version=2")

	override fun promiseFiles(): Promise<List<ServiceFile>> =
		promiseFilesAtPath(browseFilesPath, "Version=2")

	override fun promiseFiles(query: String): Promise<List<ServiceFile>> =
		promiseFilesAtPath(searchFilesPath, "Query=$query")

	override fun promiseFiles(itemId: ItemId): Promise<List<ServiceFile>> =
		promiseFilesAtPath(browseFilesPath, "ID=${itemId.id}", "Version=2")

	override fun promiseFiles(playlistId: PlaylistId): Promise<List<ServiceFile>> =
		promiseFilesAtPath(playlistFilesPath, "Playlist=${playlistId.id}")

	override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> =
		connectionProvider
			.promiseResponse("File/Played", "File=" + serviceFile.key, "FileType=Key")
			.cancelBackThen { response, _ ->
				response.use {
					val responseCode = it.code
					logger.debug("api/v1/File/Played responded with a response code of {}", responseCode)
					if (responseCode < 200 || responseCode >= 300) throw HttpResponseException(responseCode)
				}
			}

	override fun promiseRevision(): Promise<Int?> = Promise.Proxy { cp ->
		connectionProvider
			.promiseResponse("Library/GetRevision")
			.also(cp::doCancel)
			.promiseStandardResponse()
			.also(cp::doCancel)
			.then { standardRequest ->
				standardRequest.items["Sync"]
					?.takeIf { revisionValue -> revisionValue.isNotEmpty() }
					?.toInt()
			}
	}

	private fun promiseFileStringList(option: FileListParameters.Options, path: String, vararg params: String): Promise<String> =
		Promise.Proxy { cp ->
			connectionProvider.promiseResponse(
				path,
				*FileListParameters.Helpers.processParams(
					option,
					*params
				)
			).also(cp::doCancel).promiseStringBody()
		}

	private fun promiseFilesAtPath(path: String, vararg params: String): Promise<List<ServiceFile>> =
		Promise.Proxy { cp ->
			promiseFileStringList(option=FileListParameters.Options.None, path=path, *params)
				.also(cp::doCancel)
				.eventually(FileResponses)
				.also(cp::doCancel)
				.then(FileResponses)
		}

	private class FilePropertiesPromise(
		connectionProvider: ProvideConnections,
		private val serviceFile: ServiceFile
	) :
		Promise<Map<String, String>>(),
		PromisedResponse<HttpResponse, Unit>,
		CancellableMessageWriter<Unit>,
		ImmediateResponse<Throwable, Unit>
	{
		private val cancellationProxy = CancellationProxy()
		private lateinit var responseString: String

		init {
			awaitCancellation(cancellationProxy)
			val filePropertiesResponse = connectionProvider.promiseResponse("File/GetInfo", "File=" + serviceFile.key)
			val promisedProperties = filePropertiesResponse.eventually(this)

			// Handle cancellation errors directly in stack so that they don't become unhandled
			promisedProperties.excuse(this)

			cancellationProxy.doCancel(filePropertiesResponse)
		}

		override fun promiseResponse(response: HttpResponse): Promise<Unit> {
			responseString = response.use {
				if (cancellationProxy.isCancelled) {
					reject(FilePropertiesCancellationException(serviceFile))
					return Unit.toPromise()
				}

				it.bodyString
			}

			return ThreadPools.compute.preparePromise(this)
		}

		override fun prepareMessage(cancellationSignal: CancellationSignal) {
			if (cancellationSignal.isCancelled) {
				reject(FilePropertiesCancellationException(serviceFile))
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
									reject(FilePropertiesCancellationException(serviceFile))
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
				reject(FilePropertiesCancellationException(serviceFile))
			} else {
				reject(rejection)
			}
		}

		private class FilePropertiesCancellationException(serviceFile: ServiceFile)
			: CancellationException("Getting file properties cancelled for $serviceFile.")
	}

	private class ItemFilePromise(
		connectionProvider: ProvideConnections,
		itemId: ItemId?,
	) : Promise.Proxy<List<Item>>(), PromisedResponse<Document, List<Item>> {
		init {
			proxy(
				connectionProvider
					.run {
						itemId
							?.run {
								promiseResponse(
									browseLibraryParameter,
									"ID=$id",
									"Version=2",
									"ErrorOnMissing=1"
								)
							}
							?: promiseResponse(browseLibraryParameter, "Version=2", "ErrorOnMissing=1")
					}
					.also(::doCancel)
					.promiseStringBody()
					.also(::doCancel)
					.promiseXmlDocument()
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(document: Document): Promise<List<Item>> = ThreadPools.compute.preparePromise { cs ->
			val body = document.getElementsByTag("Response").firstOrNull()
				?: throw IOException("Response tag not found")

			val status = body.attr("Status")
			if (status.equals("Failure", ignoreCase = true))
				throw IOException("Server returned 'Failure'.")

			body
				.getElementsByTag("Item")
				.map { el ->
					if (cs.isCancelled) throw itemParsingCancelledException()
					Item(el.wholeOwnText().toInt(), el.attr("Name"))
				}
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}
}
