package com.lasthopesoftware.bluewater.client.connection.okhttp;

import android.os.Build;
import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier;
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.net.ssl.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.*;

public class OkHttpFactory implements ProvideOkHttpClients {

	private static final CreateAndHold<ExecutorService> executor = new Lazy<>(() -> {
		final int maxDownloadThreadPoolSize = 6;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return new ForkJoinPool(
				maxDownloadThreadPoolSize,
				ForkJoinPool.defaultForkJoinWorkerThreadFactory,
				null, true);
		}

		return new ThreadPoolExecutor(
			0, 6,
			1, TimeUnit.MINUTES,
			new LinkedBlockingQueue<>());
	});

	private static final CreateAndHold<OkHttpClient.Builder> lazyCommonBuilder = new AbstractSynchronousLazy<OkHttpClient.Builder>() {
		@Override
		protected OkHttpClient.Builder create() {
			return new OkHttpClient.Builder()
				.addNetworkInterceptor(chain -> {
					final Request.Builder requestBuilder = chain.request().newBuilder().addHeader("Connection", "close");
					return chain.proceed(requestBuilder.build());
				})
				.cache(null)
				.readTimeout(3, TimeUnit.MINUTES)
				.connectTimeout(5, TimeUnit.SECONDS)
				.dispatcher(new Dispatcher(executor.getObject()));
		}
	};

	private static final CreateAndHold<OkHttpFactory> lazyHttpFactory = new Lazy<>(OkHttpFactory::new);

	public static OkHttpFactory getInstance() {
		return lazyHttpFactory.getObject();
	}

	private OkHttpFactory() {}

	@Override
	public OkHttpClient getOkHttpClient(IUrlProvider urlProvider) {
		return lazyCommonBuilder.getObject()
			.addNetworkInterceptor(chain -> {
				final Request.Builder requestBuilder = chain.request().newBuilder();

				final String authCode = urlProvider.getAuthCode();

				if (authCode != null && !authCode.isEmpty())
					requestBuilder.addHeader("Authorization", "basic " + urlProvider.getAuthCode());

				return chain.proceed(requestBuilder.build());
			})
			.sslSocketFactory(getSslSocketFactory(urlProvider), getTrustManager(urlProvider))
			.hostnameVerifier(getHostnameVerifier(urlProvider))
			.build();
	}

	private static SSLSocketFactory getSslSocketFactory(IUrlProvider urlProvider) {
		final SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		try {
			sslContext.init(null, new TrustManager[] { getTrustManager(urlProvider) }, null);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}

		return sslContext.getSocketFactory();
	}

	private static X509TrustManager getTrustManager(IUrlProvider urlProvider) {
		final TrustManagerFactory trustManagerFactory;
		try {
			trustManagerFactory = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		try {
			trustManagerFactory.init((KeyStore) null);
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		}

		final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
			throw new IllegalStateException("Unexpected default trust managers:"
				+ Arrays.toString(trustManagers));
		}

		final X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		return urlProvider.getCertificateFingerprint().length == 0
			? trustManager
			: new SelfSignedTrustManager(urlProvider.getCertificateFingerprint(), trustManager);
	}

	private static HostnameVerifier getHostnameVerifier(IUrlProvider urlProvider) {
		final HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

		try {
			return urlProvider.getCertificateFingerprint().length == 0
				? defaultHostnameVerifier
				: new AdditionalHostnameVerifier(new URL(urlProvider.getBaseUrl()).getHost(), defaultHostnameVerifier);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
