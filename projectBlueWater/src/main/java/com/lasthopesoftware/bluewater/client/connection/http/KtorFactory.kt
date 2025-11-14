package com.lasthopesoftware.bluewater.client.connection.http

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.lasthopesoftware.resources.io.KtorIoReadableStream
import com.lasthopesoftware.resources.io.PromisingChannel
import com.lasthopesoftware.resources.io.PromisingReadableStream
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.util.appendAll
import io.ktor.util.toMap
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.net.URL
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class KtorFactory(private val context: Context) : ProvideHttpPromiseClients {
	companion object {
		private val logger by lazyLogger<KtorFactory>()
		private val buildConnectionTime = 10.seconds
		private val dispatcher by lazy { ThreadPools.io.asCoroutineDispatcher() +  CoroutineName("KtorFactory") }
		private val scope by lazy { CoroutineScope(dispatcher) }
		private fun HttpClientConfig<CIOEngineConfig>.configureForStreaming() =
			clone()
				.apply {
					install(HttpTimeout) {
						requestTimeoutMillis = null
						socketTimeoutMillis = 10.minutes.inWholeMilliseconds
						connectTimeoutMillis = 45.seconds.inWholeMilliseconds
					}
				}
		private val trustManager by lazy {
			val trustManagerFactory = try {
				TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
			} catch (e: NoSuchAlgorithmException) {
				throw RuntimeException(e)
			}

			try {
				trustManagerFactory.init(null as KeyStore?)
			} catch (e: KeyStoreException) {
				throw RuntimeException(e)
			}

			val trustManagers = trustManagerFactory.trustManagers
			check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
				("Unexpected default trust managers:" + trustManagers.contentToString())
			}

			trustManagers[0] as X509TrustManager
		}
	}

	private val commonConfiguration by lazy {
		HttpClientConfig<CIOEngineConfig>().apply {
			install(UserAgent) {
				agent = getUserAgent()
			}

			install(createClientPlugin("CloseConnection") {
				onRequest { request, _ ->
					request.header(HttpHeaders.Connection, "close")
				}
			})

			install(HttpTimeout) {
				requestTimeoutMillis = 1.minutes.inWholeMilliseconds
			}
		}
	}

	override fun promiseClient(): Promise<HttpPromiseClient> = KtorPromiseClient(
		scope,
		CIO,
		commonConfiguration.clone().apply {
			install(HttpTimeout) {
				connectTimeoutMillis = buildConnectionTime.inWholeMilliseconds
			}
		}
	).toPromise()

	inner class MediaCenterClient() : ProvideHttpPromiseServerClients<MediaCenterConnectionDetails> {

		override fun promiseServerClient(connectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient> =
			KtorPromiseClient(scope, CIO, getHttpConfiguration(connectionDetails), getEngineConfig(connectionDetails)).toPromise()

		override fun promiseStreamingServerClient(connectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient> {
			return KtorPromiseClient(
				scope,
				CIO,
				getHttpConfiguration(connectionDetails).configureForStreaming(),
				{
					getEngineConfig(connectionDetails)(this)
				}
			).toPromise()
		}

		private fun getEngineConfig(mediaCenterConnectionDetails: MediaCenterConnectionDetails): CIOEngineConfig.() -> Unit = {
			// Hostname verifier?
			https {
				// Ktor TLS network `HostnameUtils` scans for this host in the certificate if it is set
				serverName = mediaCenterConnectionDetails.baseUrl.host
				trustManager = getTrustManager(mediaCenterConnectionDetails)
			}
		}

		private fun getHttpConfiguration(mediaCenterConnectionDetails: MediaCenterConnectionDetails) = commonConfiguration.clone().apply {
			mediaCenterConnectionDetails.authCode.takeUnless { it.isNullOrEmpty() }?.let { "basic $it" }.also {
				install(createClientPlugin("BasicAuth") {
					onRequest { request, _ ->
						request.header(HttpHeaders.Authorization, it)
					}
				})
			}
		}

		private fun getTrustManager(mediaCenterConnectionDetails: MediaCenterConnectionDetails): X509TrustManager {
			return mediaCenterConnectionDetails.certificateFingerprint
				.takeIf { it.isNotEmpty() }
				?.let { fingerprint -> SelfSignedTrustManager(fingerprint, trustManager) }
				?: trustManager
		}
	}

	inner class SubsonicClient() : ProvideHttpPromiseServerClients<SubsonicConnectionDetails> {
		override fun promiseServerClient(connectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> =
			KtorPromiseClient(
				scope,
				CIO,
				getHttpConfiguration(connectionDetails),
				getEngineConfig(connectionDetails)

			).toPromise()

		override fun promiseStreamingServerClient(connectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> {
			val clientConfig = getHttpConfiguration(connectionDetails).configureForStreaming()
			return KtorPromiseClient(
				scope,
				CIO,
				clientConfig,
				{
					getEngineConfig(connectionDetails)(this)
				}
			).toPromise()
		}

		private fun getEngineConfig(subsonicConnectionDetails: SubsonicConnectionDetails): CIOEngineConfig.() -> Unit = {
			// Hostname verifier?
			https {
				// Ktor TLS network `HostnameUtils` scans for this host in the certificate if it is set
				serverName = subsonicConnectionDetails.baseUrl.host
				trustManager = getTrustManager(subsonicConnectionDetails)
			}
		}

		@OptIn(ExperimentalStdlibApi::class)
		private fun getHttpConfiguration(subsonicConnectionDetails: SubsonicConnectionDetails) = commonConfiguration.clone().apply {
			engine {
				// Hostname verifier?
				https {
					// Ktor TLS network `HostnameUtils` scans for this host in the certificate if it is set
					serverName = subsonicConnectionDetails.baseUrl.host
					trustManager = getTrustManager(subsonicConnectionDetails)
				}
			}

			install(createClientPlugin("SubsonicParams") {
				onRequest { request, _ ->
					request.url {
						with(subsonicConnectionDetails) {
							parameters["u"] = userName
							parameters["v"] = "1.4.0"
							parameters["t"] = "$password$salt".hashString("MD5").toHexString()
							parameters["s"] = salt
							parameters["c"] = BuildConfig.APPLICATION_ID
						}
					}
				}
			})
		}

		private fun getTrustManager(subsonicConnectionDetails: SubsonicConnectionDetails): X509TrustManager {
			return subsonicConnectionDetails.certificateFingerprint
				.takeIf { it.isNotEmpty() }
				?.let { fingerprint -> SelfSignedTrustManager(fingerprint, trustManager) }
				?: trustManager
		}
	}

	private fun String.hashString(algorithm: String): ByteArray =
		MessageDigest.getInstance(algorithm).digest(toByteArray(UTF_8))

	private fun getUserAgent(): String {
		val versionName = try {
			val packageName = context.packageName
			val info = context.packageManager.getPackageInfo(packageName, 0)
			info.versionName ?: "?"
		} catch (_: PackageManager.NameNotFoundException) {
			"?"
		}

		val applicationName = context.getString(R.string.app_name)
		return "$applicationName/$versionName (Linux;Android ${Build.VERSION.RELEASE})"
	}

	private class KtorHttpResponse(
		private val response: io.ktor.client.statement.HttpResponse,
		override val body: PromisingReadableStream
	) : HttpResponse {
		override val code: Int
			get() = response.status.value
		override val message: String = ""
		override val headers by lazy { response.headers.toMap() }
		override val contentLength: Long
			get() = response.contentLength() ?: 0L

		override fun promiseClose(): Promise<Unit> =
			body.promiseClose()

		override fun toString(): String {
			return "KtorHttpResponse(response=$response, code=$code, message='$message', headers=$headers, contentLength=$contentLength)"
		}
	}

	private class KtorPromiseRequest(
		scope: CoroutineScope,
		private val client: HttpClient,
		private val method: String,
		private val headers: Map<String, String>,
		private val url: URL
	) : Promise<HttpResponse>(), CancellationResponse {
		companion object {
			private fun requestCancelledException() = CancellationException("Request cancelled.")
		}

		@Volatile
		private var isStarted = false

		private val job = scope.launch {
			isStarted = true
			try {
				client.use { httpClient ->
					val request = httpClient.prepareRequest {
						url(this@KtorPromiseRequest.url)
						this.method = HttpMethod(this@KtorPromiseRequest.method)
						this.headers.appendAll(this@KtorPromiseRequest.headers)
					}

					if (BuildConfig.DEBUG) {
						logger.debug("Executing request for URL {}...", url)
					}

					request.execute { response ->
						val channel = response.bodyAsChannel()
						val ktorIoReadableStream = KtorIoReadableStream(channel, this)
						val httpResponse = KtorHttpResponse(response, ktorIoReadableStream)
						if (BuildConfig.DEBUG) {
							logger.debug("Resolving response for URL {}: {}", url, httpResponse)
						}
						resolve(httpResponse)

						// Wait for stream to be closed before closing context
						ktorIoReadableStream.suspend()
					}
				}
			} catch (e: Throwable) {
				reject(e)
			}
		}

		init {
			awaitCancellation(this)
		}

		override fun cancellationRequested() {
			job.cancel()
			if (!isStarted)
				reject(requestCancelledException())
		}
	}

	private class KtorChanneledRequest(
		scope: CoroutineScope,
		private val client: HttpClient,
		private val method: String,
		private val headers: Map<String, String>,
		private val url: URL
	) : Promise<HttpResponse>(), CancellationResponse {
		companion object {
			private fun requestCancelledException() = CancellationException("Request cancelled.")
		}

		@Volatile
		private var isStarted = false

		private val promisingChannel by lazy { PromisingChannel() }

		private val promisingStream
			get() = promisingChannel.writableStream

		private val job = scope.launch {
			isStarted = true
			try {
				client.use { httpClient ->
					val request = httpClient.prepareRequest {
						url(this@KtorChanneledRequest.url)
						this.method = HttpMethod(this@KtorChanneledRequest.method)
						this.headers.appendAll(this@KtorChanneledRequest.headers)
					}

					if (BuildConfig.DEBUG) {
						logger.debug("Executing request for URL {}...", url)
					}

					request.execute { response ->
						val httpResponse = KtorHttpResponse(response, promisingChannel)

						if (BuildConfig.DEBUG) logger.debug("Resolving response for URL {}: {}", url, httpResponse)

						resolve(httpResponse)

						try {
							val channel = response.bodyAsChannel()
							with (channel) {
								val contentLength = response.contentLength()?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
								val bufferSize = contentLength?.coerceAtMost(DEFAULT_BUFFER_SIZE) ?: DEFAULT_BUFFER_SIZE
								val buffer = ByteArray(bufferSize)
								val offset = 0
								while (!isClosedForRead) {
									val length = channel.readAvailable(buffer)
									if (length < 0) break

									if (BuildConfig.DEBUG) {
										logger.debug("Writing {} bytes to stream...", length)
									}

									val written = promisingStream.promiseWrite(buffer, offset, length).suspend()

									if (BuildConfig.DEBUG) {
										logger.debug("{} bytes written to stream.", written)
									}
								}
							}

							channel.closedCause?.let(promisingStream::closeWithCause) ?:
							promisingStream.promiseClose().suspend()
						} catch(e: Throwable) {
							promisingStream.closeWithCause(e)
						}
					}
				}
			} catch (e: Throwable) {
				reject(e)
			}
		}

		init {
			awaitCancellation(this)
		}

		override fun cancellationRequested() {
			job.cancel()
			if (!isStarted)
				reject(requestCancelledException())
		}
	}

	private class KtorPromiseClient<T : HttpClientEngineConfig>(private val scope: CoroutineScope, private val engineFactory: HttpClientEngineFactory<T>, private val httpClientConfig: HttpClientConfig<T>, private val engineConfig: T.() -> Unit = {}) : HttpPromiseClient {
		override fun promiseResponse(url: URL): Promise<HttpResponse> = promiseResponse(HttpMethod.Get.value, emptyMap(), url)

		override fun promiseResponse(method: String, headers: Map<String, String>, url: URL): Promise<HttpResponse> =
			KtorPromiseRequest(
				scope,
				getClient(),
				method,
				headers,
				url
			)

		private fun getClient() = HttpClient(engineFactory) {
			engine(engineConfig)
			this += httpClientConfig
		}
	}
}
