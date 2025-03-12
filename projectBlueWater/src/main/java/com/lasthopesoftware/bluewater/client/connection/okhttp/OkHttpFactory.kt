package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.resources.executors.ThreadPools
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.joda.time.Duration
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object OkHttpFactory : ProvideOkHttpClients {
	private val buildConnectionTime = Duration.standardSeconds(10)

    override fun getOkHttpClient(urlProvider: ProvideUrls): OkHttpClient =
        commonClient
			.newBuilder()
            .addNetworkInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val authCode = urlProvider.authCode
                if (!authCode.isNullOrEmpty()) requestBuilder.header(
                    "Authorization",
                    "basic ${urlProvider.authCode}"
                )
                chain.proceed(requestBuilder.build())
            }
            .sslSocketFactory(getSslSocketFactory(urlProvider), getTrustManager(urlProvider))
            .hostnameVerifier(getHostnameVerifier(urlProvider))
            .build()

	override fun getJriverCentralClient(): OkHttpClient =
		commonClient
			.newBuilder()
			.connectTimeout(buildConnectionTime.millis, TimeUnit.MILLISECONDS)
			.build()

	private val dispatcher by lazy { Dispatcher(ThreadPools.io) }

	private val commonClient by lazy {
		OkHttpClient.Builder()
			.addNetworkInterceptor { chain ->
				val requestBuilder =
					chain.request().newBuilder().header("Connection", "close")
				chain.proceed(requestBuilder.build())
			}
			.cache(null)
			.readTimeout(1, TimeUnit.MINUTES)
			.retryOnConnectionFailure(false)
			.dispatcher(dispatcher)
			.build()
	}

	private fun getSslSocketFactory(urlProvider: ProvideUrls): SSLSocketFactory {
		val sslContext = try {
			SSLContext.getInstance("TLS")
		} catch (e: NoSuchAlgorithmException) {
			throw RuntimeException(e)
		}

		try {
			sslContext.init(null, arrayOf<TrustManager>(getTrustManager(urlProvider)), null)
		} catch (e: KeyManagementException) {
			throw RuntimeException(e)
		}

		return sslContext.socketFactory
	}

	private fun getTrustManager(urlProvider: ProvideUrls): X509TrustManager {
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
			("Unexpected default trust managers:" + Arrays.toString(trustManagers))
		}

		val trustManager = trustManagers[0] as X509TrustManager
		return urlProvider.certificateFingerprint
			?.takeIf { it.isNotEmpty() }
			?.let { fingerprint -> SelfSignedTrustManager(fingerprint, trustManager) }
			?: trustManager
	}

	private fun getHostnameVerifier(urlProvider: ProvideUrls): HostnameVerifier {
		val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
		return urlProvider.certificateFingerprint
			?.takeIf { it.isNotEmpty() }
			?.let {
				urlProvider.baseUrl.host?.let { host ->
					AdditionalHostnameVerifier(host, defaultHostnameVerifier)
				}
			}
			?: defaultHostnameVerifier
	}
}
