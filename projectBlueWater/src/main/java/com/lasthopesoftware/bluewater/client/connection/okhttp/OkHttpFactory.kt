package com.lasthopesoftware.bluewater.client.connection.okhttp

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.resources.executors.ThreadPools
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

object OkHttpFactory : ProvideOkHttpClients {
    override fun getOkHttpClient(urlProvider: IUrlProvider): OkHttpClient =
        commonBuilder
            .addNetworkInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val authCode = urlProvider.authCode
                if (authCode != null && authCode.isNotEmpty()) requestBuilder.addHeader(
                    "Authorization",
                    "basic ${urlProvider.authCode}"
                )
                chain.proceed(requestBuilder.build())
            }
            .sslSocketFactory(getSslSocketFactory(urlProvider), getTrustManager(urlProvider))
            .hostnameVerifier(getHostnameVerifier(urlProvider))
            .build()

	private val dispatcher by lazy { Dispatcher(ThreadPools.io) }

	private val commonBuilder by lazy {
		OkHttpClient.Builder()
			.addNetworkInterceptor { chain ->
				val requestBuilder =
					chain.request().newBuilder().addHeader("Connection", "close")
				chain.proceed(requestBuilder.build())
			}
			.cache(null)
			.readTimeout(1, TimeUnit.MINUTES)
			.retryOnConnectionFailure(false)
			.dispatcher(dispatcher)
	}

	private fun getSslSocketFactory(urlProvider: IUrlProvider): SSLSocketFactory {
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

	private fun getTrustManager(urlProvider: IUrlProvider): X509TrustManager {
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

	private fun getHostnameVerifier(urlProvider: IUrlProvider): HostnameVerifier {
		val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
		return urlProvider.certificateFingerprint
			?.takeIf { it.isNotEmpty() }
			?.let {
				urlProvider.baseUrl?.host?.let { host ->
					AdditionalHostnameVerifier(host, defaultHostnameVerifier)
				}
			}
			?: defaultHostnameVerifier
	}
}
