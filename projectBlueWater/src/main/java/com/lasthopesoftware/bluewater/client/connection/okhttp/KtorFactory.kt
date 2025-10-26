package com.lasthopesoftware.bluewater.client.connection.okhttp

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
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
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
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.util.appendAll
import io.ktor.util.toMap
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import java.io.InputStream
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
		private val buildConnectionTime = 10.seconds
		private val dispatcher by lazy { ThreadPools.io.asCoroutineDispatcher() +  CoroutineName("KtorFactory") }
		private val scope by lazy { CoroutineScope(dispatcher) }
		private fun HttpClientConfig<CIOEngineConfig>.configureForStreaming() =
			clone()
				.apply {
					engine {
						endpoint.connectAttempts = 2
					}
					install(HttpTimeout) {
						requestTimeoutMillis = 45.seconds.inWholeMilliseconds
					}
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
			KtorPromiseClient(scope, CIO, getHttpConfiguration(connectionDetails)).toPromise()

		override fun promiseStreamingServerClient(connectionDetails: MediaCenterConnectionDetails): Promise<HttpPromiseClient> {
			val clientConfig = getHttpConfiguration(connectionDetails)
				.configureForStreaming()
			return KtorPromiseClient(scope, CIO, clientConfig).toPromise()
		}

		private fun getHttpConfiguration(mediaCenterConnectionDetails: MediaCenterConnectionDetails) = commonConfiguration.clone().apply {
			engine {

				// Hostname verifier?
				https {
					// Ktor TLS network `HostnameUtils` scans for this host in the certificate if it is set
					serverName = mediaCenterConnectionDetails.baseUrl.host
					trustManager = getTrustManager(mediaCenterConnectionDetails)
				}
			}

			mediaCenterConnectionDetails.authCode.takeUnless { it.isNullOrEmpty() }?.let { "basic $it" }.also {
				install(createClientPlugin("BasicAuth") {
					onRequest { request, _ ->
						request.header(HttpHeaders.Authorization, it)
					}
				})
			}
		}

		private fun getTrustManager(mediaCenterConnectionDetails: MediaCenterConnectionDetails): X509TrustManager {
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

			val trustManager = trustManagers[0] as X509TrustManager
			return mediaCenterConnectionDetails.certificateFingerprint
				.takeIf { it.isNotEmpty() }
				?.let { fingerprint -> SelfSignedTrustManager(fingerprint, trustManager) }
				?: trustManager
		}
	}

	inner class SubsonicClient() : ProvideHttpPromiseServerClients<SubsonicConnectionDetails> {
		override fun promiseServerClient(connectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> =
			KtorPromiseClient(scope, CIO, getHttpConfiguration(connectionDetails))
				.toPromise()

		override fun promiseStreamingServerClient(connectionDetails: SubsonicConnectionDetails): Promise<HttpPromiseClient> {
			val clientConfig = getHttpConfiguration(connectionDetails).configureForStreaming()
			return KtorPromiseClient(scope, CIO,  clientConfig).toPromise()
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

			val trustManager = trustManagers[0] as X509TrustManager
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

	private class KtorHttpResponse(private val client: HttpClient, private val response: io.ktor.client.statement.HttpResponse, override val body: InputStream) : HttpResponse {
		override val code: Int
			get() = response.status.value
		override val message: String = ""
		override val headers by lazy { response.headers.toMap() }
		override val contentLength: Long
			get() = response.contentLength() ?: 0L

		override fun close() = client.close()
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private class KtorPromiseClient<T : HttpClientEngineConfig>(private val scope: CoroutineScope, private val engineFactory: HttpClientEngineFactory<T>, private val httpClientConfig: HttpClientConfig<T>) : HttpPromiseClient {
		override fun promiseResponse(url: URL): Promise<HttpResponse> =
			scope.async {
				val httpClient = getClient()
				val response = httpClient.request(url = url)
				KtorHttpResponse(httpClient, response, response.bodyAsChannel().toInputStream())
			}.toPromise()

		override fun promiseResponse(method: String, headers: Map<String, String>, url: URL): Promise<HttpResponse> =
			scope.async {
				val httpClient = getClient()
				val response = httpClient.request {
					url(url)
					this.method = HttpMethod(method)
					this.headers.appendAll(headers)
				}
				KtorHttpResponse(httpClient, response, response.bodyAsChannel().toInputStream())
			}.toPromise()

		private fun getClient() = HttpClient(engineFactory) {
			this += httpClientConfig
		}
	}
}
