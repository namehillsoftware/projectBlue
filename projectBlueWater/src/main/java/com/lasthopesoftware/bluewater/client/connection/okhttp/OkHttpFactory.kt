package com.lasthopesoftware.bluewater.client.connection.okhttp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.requests.HttpPromiseClient
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.compilation.DebugFlag
import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.cancellation.CancellationResponse
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.joda.time.Duration
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class OkHttpFactory(private val context: Context) : ProvideHttpPromiseClients, ProvideOkHttpClients {
	companion object {
		private val buildConnectionTime = Duration.standardSeconds(10)
		private val dispatcher by lazy { Dispatcher(ThreadPools.io) }
	}

	override fun getServerClient(serverConnection: ServerConnection): HttpPromiseClient =
		OkHttpPromiseClient(getOkHttpClient(serverConnection))

	override fun getClient(): HttpPromiseClient = OkHttpPromiseClient(getOkHttpClient())

	override fun getOkHttpClient(serverConnection: ServerConnection): OkHttpClient =
		commonClient
			.newBuilder()
			.addNetworkInterceptor { chain ->
				val requestBuilder = chain.request().newBuilder()
				val authCode = serverConnection.authCode
				if (!authCode.isNullOrEmpty()) requestBuilder.header(
					"Authorization",
					"basic $authCode"
				)
				chain.proceed(requestBuilder.build())
			}
			.sslSocketFactory(getSslSocketFactory(serverConnection), getTrustManager(serverConnection))
			.hostnameVerifier(getHostnameVerifier(serverConnection))
			.build()

	private fun getOkHttpClient(): OkHttpClient =
		commonClient
			.newBuilder()
			.connectTimeout(buildConnectionTime.millis, TimeUnit.MILLISECONDS)
			.build()

	private val commonClient by lazy {
		OkHttpClient.Builder()
			.addNetworkInterceptor { chain ->
				val requestBuilder =
					chain
						.request()
						.newBuilder()
						.header("Connection", "close")
						.header("User-Agent", getUserAgent())
				chain.proceed(requestBuilder.build())
			}
			.cache(null)
			.readTimeout(1, TimeUnit.MINUTES)
			.retryOnConnectionFailure(false)
			.dispatcher(dispatcher)
			.build()
	}

	private fun getSslSocketFactory(serverConnection: ServerConnection): SSLSocketFactory {
		val sslContext = try {
			SSLContext.getInstance("TLS")
		} catch (e: NoSuchAlgorithmException) {
			throw RuntimeException(e)
		}

		try {
			sslContext.init(null, arrayOf<TrustManager>(getTrustManager(serverConnection)), null)
		} catch (e: KeyManagementException) {
			throw RuntimeException(e)
		}

		return sslContext.socketFactory
	}

	private fun getTrustManager(serverConnection: ServerConnection): X509TrustManager {
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
		return serverConnection.certificateFingerprint
			.takeIf { it.isNotEmpty() }
			?.let { fingerprint -> SelfSignedTrustManager(fingerprint, trustManager) }
			?: trustManager
	}

	private fun getHostnameVerifier(serverConnection: ServerConnection): HostnameVerifier {
		val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
		return serverConnection.certificateFingerprint
			.takeIf { it.isNotEmpty() }
			?.let {
				serverConnection.baseUrl.host?.let { host ->
					AdditionalHostnameVerifier(host, defaultHostnameVerifier)
				}
			}
			?: defaultHostnameVerifier
	}

	private class OkHttpResponse(private val response: Response) : HttpResponse {
		override val code: Int
			get() = response.code
		override val message: String
			get() = response.message
		override val body: InputStream
			get() = response.body.byteStream()

		override fun close() = response.closeQuietly()
	}

	private class HttpResponsePromise(private val call: Call) : Promise<HttpResponse>(), Callback, CancellationResponse {

		companion object {
			private val logger by lazyLogger<HttpResponsePromise>()
		}

		init {
			awaitCancellation(this)
			call.enqueue(this)
		}

		override fun onFailure(call: Call, e: IOException) = reject(e)

		override fun onResponse(call: Call, response: Response) {
			if (DebugFlag.isDebugCompilation && !response.isSuccessful && logger.isDebugEnabled) {
				logger.debug("Response returned error code {}.", response.code)
			}

			resolve(OkHttpResponse(response))
		}

		override fun cancellationRequested() = call.cancel()
	}

	private class OkHttpPromiseClient(private val okHttpClient: OkHttpClient) : HttpPromiseClient {
		override fun promiseResponse(url: URL): Promise<HttpResponse> =
			try {
				HttpResponsePromise(okHttpClient.newCall(Request.Builder().url(url).build()))
			} catch (e: Throwable) {
				Promise(e)
			}
	}

	private fun getUserAgent(): String {
		val versionName = try {
			val packageName = context.packageName
			val info = context.packageManager.getPackageInfo(packageName, 0)
			info.versionName ?: "?"
		} catch (e: PackageManager.NameNotFoundException) {
			"?"
		}

		val applicationName = context.getString(R.string.app_name)
		return ("$applicationName/$versionName (Linux;Android ${Build.VERSION.RELEASE})")
	}
}
