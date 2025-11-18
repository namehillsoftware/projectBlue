package com.lasthopesoftware.bluewater.client.connection.live

import android.os.Build
import androidx.media3.datasource.DataSource
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyHelpers.durationInMs
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LookupFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.lasthopesoftware.bluewater.client.connection.requests.bodyString
import com.lasthopesoftware.bluewater.client.connection.requests.isSuccessful
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withMcApi
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideServerHttpDataSource
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.exceptions.HttpResponseException
import com.lasthopesoftware.bluewater.shared.StandardResponse.Companion.toStandardResponse
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.policies.retries.RetryOnRejectionLazyPromise
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.closables.eventuallyUse
import com.lasthopesoftware.resources.closables.thenUse
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.NonStandardResponseException
import com.lasthopesoftware.resources.io.PromisingReadableStream
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
import java.io.IOException
import java.net.URL
import java.util.concurrent.CancellationException
import kotlin.math.pow

class LiveMediaCenterConnection(
	private val mediaCenterConnectionDetails: MediaCenterConnectionDetails,
	private val httpPromiseClients: ProvideHttpPromiseServerClients<MediaCenterConnectionDetails>,
	private val serverHttpDataSource: ProvideServerHttpDataSource<MediaCenterConnectionDetails>,
) : LiveServerConnection, RemoteLibraryAccess
{
	companion object {
		private val logger by lazyLogger<LiveMediaCenterConnection>()
		private const val browseFilesPath = "Browse/Files"
		private const val playlistFilesPath = "Playlist/Files"
		private const val searchFilesPath = "Files/Search"
		private const val browseLibraryPath = "Browse/Children"
		private const val imageFormat = "jpg"
		private const val serializedFileListParameter = "Action=Serialize"
		private const val shuffleFileListParameter = "Shuffle=1"

		private val editableFilePropertyDefinitions by lazy { FilePropertyDefinition.EditableFilePropertyDefinition.entries.toSet() }
	}

	private object KnownFileProperties {
		const val peakLevelSample = "Peak Level (Sample)"
	}

	private val mcApiUrl by lazy { mediaCenterConnectionDetails.baseUrl.withMcApi() }

	private val httpClient by RetryOnRejectionLazyPromise { httpPromiseClients.promiseServerClient(mediaCenterConnectionDetails) }

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

	private val promisedDataSourceFactory by RetryOnRejectionLazyPromise {
		serverHttpDataSource.promiseDataSourceFactory(mediaCenterConnectionDetails)
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

	override fun promiseDataSourceFactory(): Promise<DataSource.Factory> = promisedDataSourceFactory

	override val dataAccess: RemoteLibraryAccess
		get() = this

	override fun promiseIsConnectionPossible(): Promise<Boolean> = ConnectionPossiblePromise()

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<LookupFileProperties> =
		FilePropertiesPromise(serviceFile)

	override fun promiseFilePropertyUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		promiseResponse("File/SetInfo", "File=${serviceFile.key}", "Field=$property", "Value=$value", "formatted=" + if (isFormatted) "1" else "0")
			.cancelBackEventually { response ->
				response.thenUse {
					logger.info("api/v1/File/SetInfo responded with a response code of {}.", it.code)
				}
			}

	override fun promiseItems(itemId: KeyedIdentifier?): Promise<List<IItem>> = when (itemId) {
		is ItemId -> ItemListPromise(itemId)
		null -> ItemListPromise(null)
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

	override fun promiseFile(serviceFile: ServiceFile): Promise<PromisingReadableStream> =
		Promise.Proxy { cp ->
			httpClient
				.eventually { it.promiseResponse(getFileUrl(serviceFile)) }
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
			).also(cp::doCancel).eventually { response ->
				response?.eventuallyUse {
					if (cp.isCancelled) throw CancellationException("Cancelled while retrieving image")
					else when (response.code) {
						200 -> response.body.eventuallyUse { it.promiseReadAllBytes() }
						else -> emptyByteArray.toPromise()
					}
				} ?: emptyByteArray.toPromise()
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
		).cancelBackEventually { response ->
			response.eventuallyUse {
				when (response.code) {
					200 -> response.body.eventuallyUse { it.promiseReadAllBytes() }
					else -> emptyByteArray.toPromise()
				}
			}
		}

	override fun promiseFileStringList(itemId: ItemId?): Promise<String> =
		itemId
			?.run {
				promiseFileStringList(browseFilesPath, "ID=$id")
			}
			?: promiseFileStringList(browseFilesPath)

	override fun promiseFileStringList(playlistId: PlaylistId): Promise<String> =
		promiseFileStringList(playlistFilesPath, "Playlist=${playlistId.id}")

	override fun promiseShuffledFileStringList(itemId: ItemId?): Promise<String> =
		itemId
			?.run {
				promiseFileStringList(browseFilesPath, "ID=$id", shuffleFileListParameter)
			}
			?: promiseFileStringList(browseFilesPath, shuffleFileListParameter)

	override fun promiseShuffledFileStringList(playlistId: PlaylistId): Promise<String> = "".toPromise()

	override fun promiseFiles(): Promise<List<ServiceFile>> =
		promiseFilesAtPath(browseFilesPath)

	override fun promiseFiles(query: String): Promise<List<ServiceFile>> =
		promiseFilesAtPath(searchFilesPath, "Query=[Media Type]=[Audio] $query")

	override fun promiseFiles(itemId: ItemId): Promise<List<ServiceFile>> =
		promiseFilesAtPath(browseFilesPath, "ID=${itemId.id}")

	override fun promiseFiles(playlistId: PlaylistId): Promise<List<ServiceFile>> =
		promiseFilesAtPath(playlistFilesPath, "Playlist=${playlistId.id}")

	override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> = Promise.Proxy { cp ->
		promiseServerVersion()
			.also(cp::doCancel)
			.eventually { v ->
				if (v != null && v.major >= 22) promiseResponse("File/Played", "File=" + serviceFile.key, "FileType=Key")
					.also(cp::doCancel)
					.eventually { response ->
						response.thenUse {
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
				standardRequest.items["Master"]
					?.takeIf { revisionValue -> revisionValue.isNotEmpty() }
					?.toLong()
			}
	}

	private fun promiseFilesAtPath(path: String, vararg params: String): Promise<List<ServiceFile>> =
		Promise.Proxy { cp ->
			promiseFileStringList(path, *params)
				.also(cp::doCancel)
				.eventually(FileResponses)
				.also(cp::doCancel)
				.then(FileResponses)
		}

	private fun promiseFileStringList(path: String, vararg params: String): Promise<String> =
		Promise.Proxy { cp ->
			promiseResponse(
				path,
				*params,
				serializedFileListParameter,
			).also(cp::doCancel).promiseStringBody()
		}

	private fun promiseResponse(path: String, vararg params: String): Promise<HttpResponse> {
		val url = mcApiUrl.addPath(path).addParams(*params)
		return httpClient.eventually { it.promiseResponse(url) }
	}

	private class MediaCenterFilePropertiesLookup(
		private val filePropertiesMap: MutableMap<String, String>
	) : FilePropertiesLookup() {

		companion object {
			private val editableFileProperties by lazy { editableFilePropertyDefinitions.map { it.name }.toSet() }
		}

		override val availableProperties by lazy { filePropertiesMap.keys }

		override fun getValue(name: String): String? {
			if (name != NormalizedFileProperties.PeakLevel) return filePropertiesMap[name]

			val peakLevelSampleValue = filePropertiesMap[KnownFileProperties.peakLevelSample] ?: return null
			val peakLevelSample = peakLevelSampleValue
				.split(';', limit = 2)
				.firstOrNull()?.lowercase()?.removeSuffix("db")?.trimEnd()?.toDoubleOrNull()
				?.let { 10.0.pow(it / 20) }
				?: 1.0

			return peakLevelSample.toString()
		}

		override fun isEditable(name: String): Boolean = editableFileProperties.contains(name)

		override fun updateValue(name: String, value: String) {
			filePropertiesMap[name] = value
		}
	}

	private inner class FilePropertiesPromise(private val serviceFile: ServiceFile) :
		Promise<LookupFileProperties>(),
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
			return response.eventuallyUse {
				if (cancellationProxy.isCancelled) {
					reject(filePropertiesCancellationException(serviceFile))
					return Unit.toPromise()
				}

				it.bodyString
			}.eventually {
				responseString = it
				ThreadPools.compute.preparePromise(this)
			}
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
						val map = HashMap<String, String>()
						xml
							.getElementsByTag("item")
							.firstOrNull()
							?.children()
							?.associateTo(HashMap()) { el ->
								if (cancellationSignal.isCancelled) {
									reject(filePropertiesCancellationException(serviceFile))
									return
								}

								val name = el.attr("Name")
								Pair(name, el.wholeOwnText())
							}
							?.toMap(map)

						MediaCenterFilePropertiesLookup(map)
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

	private inner class ItemListPromise(
		itemId: ItemId?,
	) : Promise.Proxy<List<IItem>>(), PromisedResponse<Document, List<IItem>> {
		init {
			val promisedResponse = itemId
				?.run {
					promiseResponse(
						browseLibraryPath,
						"ID=$id",
						"Version=2",
						"ErrorOnMissing=1"
					)
				}
				?: promiseResponse(browseLibraryPath, "Version=2", "ErrorOnMissing=1")

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

					val maybePlaylistId = el.attr("PlaylistID").takeIf { it.isNotEmpty() }?.let(::PlaylistId)

					Item(el.wholeOwnText(), el.attr("Name"), maybePlaylistId)
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
				.eventually { promiseTestedResponse(it, cancellationProxy) }
				.eventually { isPossible ->
					when {
						!isPossible -> false.toPromise()
						mediaCenterConnectionDetails.authCode.isNullOrEmpty() -> true.toPromise()
						else -> promiseResponse("Authenticate")
							.also(cancellationProxy::doCancel)
							.then { r -> r.isSuccessful }
					}
				}
				.then(
					::resolve,
					{ e ->
						logger.error("Error checking connection at URL {}.", mediaCenterConnectionDetails.baseUrl, e)
						resolve(false)
					}
				)
				.also(cancellationProxy::doCancel)
		}

		private fun promiseTestedResponse(response: HttpResponse, cancellationSignal: CancellationSignal): Promise<Boolean> {
			return response
				.eventuallyUse { r -> r.bodyString }
				.then {
					if (cancellationSignal.isCancelled) false
					else try {
						Jsoup.parse(it, Parser.xmlParser()).toStandardResponse().isStatusOk
					} catch (e: NonStandardResponseException) {
						logger.warn("Non standard response received.", e)
						false
					} catch (e: IOException) {
						logger.error("Error closing connection, device failure?", e)
						false
					} catch (e: IllegalArgumentException) {
						logger.warn("Illegal argument passed in", e)
						false
					} catch (t: Throwable) {
						logger.error("Unexpected error parsing response.", t)
						false
					}
				}
		}
	}

	private inner class FilePropertiesPlayStatsUpdatePromise(private val serviceFile: ServiceFile) :
		Promise.Proxy<Unit>(), PromisedResponse<LookupFileProperties, Unit>
	{
		init {
			proxy(
				FilePropertiesPromise(serviceFile)
					.also(::doCancel)
					.eventually(this)
			)
		}

		override fun promiseResponse(fileProperties: LookupFileProperties): Promise<Unit> {
			try {
				val lastPlayedServer = fileProperties.get(NormalizedFileProperties.LastPlayed)?.value
				val duration = fileProperties.durationInMs ?: 0
				val currentTime = System.currentTimeMillis()
				if (lastPlayedServer != null && currentTime - duration <= lastPlayedServer.toLong() * 1000) return Unit.toPromise()

				val numberPlaysString = fileProperties.get(NormalizedFileProperties.NumberPlays)?.value
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

				return whenAll(numberPlaysUpdate, lastPlayedUpdate).unitResponse()
			} catch (ne: NumberFormatException) {
				logger.error(ne.toString(), ne)
				return Unit.toPromise()
			}
		}
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

