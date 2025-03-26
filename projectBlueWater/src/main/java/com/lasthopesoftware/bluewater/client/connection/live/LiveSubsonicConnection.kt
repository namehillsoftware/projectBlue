package com.lasthopesoftware.bluewater.client.connection.live

import androidx.annotation.Keep
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.FileResponses
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.requests.bodyString
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addParams
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.addPath
import com.lasthopesoftware.bluewater.client.connection.url.UrlBuilder.withSubsonicApi
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.NonStandardResponseException
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.exceptions.isOkHttpCanceled
import com.lasthopesoftware.policies.caching.TimedExpirationPromiseCache
import com.lasthopesoftware.promises.extensions.preparePromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.fromJson
import com.lasthopesoftware.resources.io.promiseStringBody
import com.lasthopesoftware.resources.io.promiseXmlDocument
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter
import com.namehillsoftware.handoff.promises.response.ImmediateResponse
import com.namehillsoftware.handoff.promises.response.PromisedResponse
import org.joda.time.Duration
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

class LiveSubsonicConnection(
	private val subsonicConnectionDetails: SubsonicConnectionDetails,
	private val httpPromiseClients: ProvideHttpPromiseClients,
	private val okHttpClients: ProvideOkHttpClients,
) : LiveServerConnection, RemoteLibraryAccess
{
	companion object {
		private val logger by lazyLogger<LiveSubsonicConnection>()
		private const val browseFilesPath = "Browse/Files"
		private const val playlistFilesPath = "Playlist/Files"
		private const val searchFilesPath = "Files/Search"
		private const val imageFormat = "jpg"
		private val checkedExpirationTime by lazy { Duration.standardSeconds(30) }
	}

	private val subsonicApiUrl by lazy {
		with (subsonicConnectionDetails) { baseUrl.withSubsonicApi() }
	}

	private val revisionCache by lazy { TimedExpirationPromiseCache<Unit, Int?>(checkedExpirationTime) }

	private val httpClient by lazy { httpPromiseClients.getServerClient(subsonicConnectionDetails) }

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(subsonicConnectionDetails.baseUrl, key)

	override fun getFileUrl(serviceFile: ServiceFile): URL = subsonicConnectionDetails.baseUrl

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

	override fun promiseFileProperties(serviceFile: ServiceFile): Promise<Map<String, String>> = Promise(emptyMap())

	override fun promiseFilePropertyUpdate(serviceFile: ServiceFile, property: String, value: String, isFormatted: Boolean): Promise<Unit> =
		Unit.toPromise()

	override fun promiseItems(itemId: ItemId?): Promise<List<Item>> = ItemFilePromise(itemId)

	override fun promiseAudioPlaylistPaths(): Promise<List<String>> = Promise(emptyList())

	override fun promiseStoredPlaylist(playlistPath: String, playlist: List<ServiceFile>): Promise<*> = Unit.toPromise()

	override fun promiseIsReadOnly(): Promise<Boolean> = true.toPromise()

	override fun promiseServerVersion(): Promise<SemanticVersion?> = Promise.empty()

	override fun promiseFile(serviceFile: ServiceFile): Promise<InputStream> = emptyByteArray.inputStream().toPromise()

	override fun promiseImageBytes(serviceFile: ServiceFile): Promise<ByteArray> = emptyByteArray.toPromise()

	override fun promiseImageBytes(itemId: ItemId): Promise<ByteArray> = emptyByteArray.toPromise()

	override fun promiseFileStringList(itemId: ItemId?): Promise<String> = "".toPromise()

	override fun promiseShuffledFileStringList(itemId: ItemId?): Promise<String> = "".toPromise()

	override fun promiseFiles(): Promise<List<ServiceFile>> = Promise(emptyList())

	override fun promiseFiles(query: String): Promise<List<ServiceFile>> = Promise(emptyList())

	override fun promiseFiles(itemId: ItemId): Promise<List<ServiceFile>> = Promise(emptyList())

	override fun promiseFiles(playlistId: PlaylistId): Promise<List<ServiceFile>> = Promise(emptyList())

	override fun promisePlaystatsUpdate(serviceFile: ServiceFile): Promise<*> = Unit.toPromise()

	override fun promiseRevision(): Promise<Int?> = Promise.empty()

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
		val url = subsonicApiUrl.addPath(path).addParams(*params)
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

	private inner class ItemFilePromise(itemId: ItemId?) :
		Promise.Proxy<List<Item>>(),
		PromisedResponse<Document, List<Item>>
	{
		init {
			val promisedResponse = itemId
				?.run {
					promiseResponse(
						"library/playlists",
						"ID=$id"
					)
				}
				?: promiseResponse("library/playlists")

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
					Item(el.wholeOwnText(), el.attr("Name"))
				}
		}

		private fun itemParsingCancelledException() = CancellationException("Item parsing was cancelled.")
	}

	private inner class ConnectionPossiblePromise : Promise<Boolean>() {
		init {
			val cancellationProxy = CancellationProxy()
			awaitCancellation(cancellationProxy)

			httpClient
				.promiseResponse(subsonicApiUrl.addPath("ping.view"))
				.also(cancellationProxy::doCancel)
				.then(
					{ it, cp -> resolve(testResponse(it, cp)) },
					{ e, _ ->
						logger.error("Error checking connection at URL {}.", subsonicConnectionDetails.baseUrl, e)
						resolve(false)
					}
				)
				.also(cancellationProxy::doCancel)
		}

		private fun testResponse(response: HttpResponse, cancellationSignal: CancellationSignal): Boolean = response.use { r ->
			try {
				if (cancellationSignal.isCancelled || r.code != 200) return false

				val gson = Gson()
				val json = gson.fromJson<JsonObject>(r.bodyString)
				json?.get("subsonic-response")?.asJsonObject?.get("status")?.asString == "ok"
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

	@Keep
	private class SubsonicResponse(
		val status: String,
		val version: String,
		val type: String,
		val serverVersion: String,
		val openSubsonic: Boolean
	)
}
