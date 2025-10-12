package com.lasthopesoftware.bluewater.client.connection.okhttp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClientOptions
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.Promise
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
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
import kotlinx.coroutines.cancel
import java.io.InputStream
import java.net.URL
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class KtorFactory(private val context: Context) : ProvideHttpPromiseClients {
	companion object {
		private val buildConnectionTime = 10.seconds
		private val dispatcher by lazy { ThreadPools.io.asCoroutineDispatcher() +  CoroutineName("KtorFactory") }
	}

	private val scope by lazy { CoroutineScope(dispatcher) }

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

	override fun getServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails): HttpPromiseClient =
		KtorPromiseClient(scope, getHttpConfiguration(mediaCenterConnectionDetails).getClient())

	override fun getServerClient(mediaCenterConnectionDetails: MediaCenterConnectionDetails, clientOptions: HttpPromiseClientOptions): HttpPromiseClient {
		val clientConfig = getHttpConfiguration(mediaCenterConnectionDetails)
			.clone()
			.apply {
				engine {
					endpoint.connectAttempts = 2
				}
				install(HttpTimeout) {
					requestTimeoutMillis = clientOptions.readTimeout.inWholeMilliseconds
				}
			}
		return KtorPromiseClient(scope, clientConfig.getClient())
	}

	override fun getServerClient(subsonicConnectionDetails: SubsonicConnectionDetails): HttpPromiseClient =
		KtorPromiseClient(scope, getHttpConfiguration(subsonicConnectionDetails).getClient())

	override fun getServerClient(subsonicConnectionDetails: SubsonicConnectionDetails, clientOptions: HttpPromiseClientOptions): HttpPromiseClient {
		val clientConfig = getHttpConfiguration(subsonicConnectionDetails)
			.clone()
			.apply {
				engine {
					endpoint.connectAttempts = 2
				}

				install(HttpTimeout) {
					requestTimeoutMillis = clientOptions.readTimeout.inWholeMilliseconds
				}
			}
		return KtorPromiseClient(scope, clientConfig.getClient())
	}

	override fun getClient(): HttpPromiseClient = KtorPromiseClient(scope,getHttpConfiguration().getClient())

	private fun getHttpConfiguration(mediaCenterConnectionDetails: MediaCenterConnectionDetails) = commonConfiguration.clone().apply {
		engine {

			// Hostname verifier?
			https {
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

	@OptIn(ExperimentalStdlibApi::class)
	private fun getHttpConfiguration(subsonicConnectionDetails: SubsonicConnectionDetails) = commonConfiguration.clone().apply {
		engine {
			// Hostname verifier?
			https {
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

	private fun getHttpConfiguration() = commonConfiguration.clone().apply {
			install(HttpTimeout) {
				connectTimeoutMillis = buildConnectionTime.inWholeMilliseconds
			}
		}

	private fun HttpClientConfig<CIOEngineConfig>.getClient() = HttpClient(CIO) { this@HttpClient += this@getClient }

	private fun getSslSocketFactory(mediaCenterConnectionDetails: MediaCenterConnectionDetails): SSLSocketFactory {
		val sslContext = try {
			SSLContext.getInstance("TLS")
		} catch (e: NoSuchAlgorithmException) {
			throw RuntimeException(e)
		}

		try {
			sslContext.init(null, arrayOf<TrustManager>(getTrustManager(mediaCenterConnectionDetails)), null)
		} catch (e: KeyManagementException) {
			throw RuntimeException(e)
		}

		return sslContext.socketFactory
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

	private fun getHostnameVerifier(mediaCenterConnectionDetails: MediaCenterConnectionDetails): HostnameVerifier {
		val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
		return mediaCenterConnectionDetails.certificateFingerprint
			.takeIf { it.isNotEmpty() }
			?.let {
				mediaCenterConnectionDetails.baseUrl.host?.let { host ->
					AdditionalHostnameVerifier(host, defaultHostnameVerifier)
				}
			}
			?: defaultHostnameVerifier
	}

	private fun getSslSocketFactory(subsonicConnectionDetails: SubsonicConnectionDetails): SSLSocketFactory {
		val sslContext = try {
			SSLContext.getInstance("TLS")
		} catch (e: NoSuchAlgorithmException) {
			throw RuntimeException(e)
		}

		try {
			sslContext.init(null, arrayOf<TrustManager>(getTrustManager(subsonicConnectionDetails)), null)
		} catch (e: KeyManagementException) {
			throw RuntimeException(e)
		}

		return sslContext.socketFactory
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

	private fun getHostnameVerifier(subsonicConnectionDetails: SubsonicConnectionDetails): HostnameVerifier {
		val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
		return subsonicConnectionDetails.certificateFingerprint
			.takeIf { it.isNotEmpty() }
			?.let {
				subsonicConnectionDetails.baseUrl.host?.let { host ->
					AdditionalHostnameVerifier(host, defaultHostnameVerifier)
				}
			}
			?: defaultHostnameVerifier
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

	private class KtorHttpResponse(private val response: io.ktor.client.statement.HttpResponse, override val body: InputStream) : HttpResponse {
		override val code: Int
			get() = response.status.value
		override val message: String = ""
		override val headers by lazy { response.headers.toMap() }
//		override val body: InputStream
//			get() = response.bodyAsChannel().toInputStream()
		override val contentLength: Long
			get() = response.contentLength() ?: 0L

		override fun close() = response.cancel()
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private class KtorPromiseClient(private val scope: CoroutineScope, private val httpClient: HttpClient) : HttpPromiseClient {
		override fun promiseResponse(url: URL): Promise<HttpResponse> =
			scope.async {
				val response = httpClient.request(url = url)
				KtorHttpResponse(response, response.bodyAsChannel().toInputStream())
			}.toPromise()

		override fun promiseResponse(method: String, headers: Map<String, String>, url: URL): Promise<HttpResponse> =
			scope.async {
				val response = httpClient.request {
					url(url)
					this.method = HttpMethod(method)
					this.headers.appendAll(headers)
				}
				KtorHttpResponse(response, response.bodyAsChannel().toInputStream())
			}.toPromise()
	}
}
