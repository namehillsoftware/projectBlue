package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.trust.AdditionalHostnameVerifier;
import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ConnectionProvider implements IConnectionProvider {

	private final IUrlProvider urlProvider;

	private final CreateAndHold<OkHttpClient> lazyOkHttpClient = new AbstractSynchronousLazy<OkHttpClient>() {
		@Override
		protected OkHttpClient create() {
			return new OkHttpClient.Builder()
				.addNetworkInterceptor(chain -> {
					Request request = chain.request().newBuilder()
						.addHeader("Connection", "close")
						.addHeader("Authorization", "basic " + urlProvider.getAuthCode()).build();
					return chain.proceed(request);
				})
				.readTimeout(3, TimeUnit.MINUTES)
				.connectTimeout(5, TimeUnit.SECONDS)
				.sslSocketFactory(getSslSocketFactory(), getTrustManager())
				.hostnameVerifier(getHostnameVerifier())
				.build();
		}
	};

	private final CreateAndHold<X509TrustManager> lazyTrustManager = new AbstractSynchronousLazy<X509TrustManager>() {
		@Override
		protected X509TrustManager create() throws Throwable {
			final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
				TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init((KeyStore) null);
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
	};

	private final CreateAndHold<SSLSocketFactory> lazySslSocketFactory = new AbstractSynchronousLazy<SSLSocketFactory>() {
		@Override
		protected SSLSocketFactory create() throws NoSuchAlgorithmException, KeyManagementException {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { lazyTrustManager.getObject() }, null);
			return sslContext.getSocketFactory();
		}
	};

	private final CreateAndHold<HostnameVerifier> lazyHostnameVerifier = new AbstractSynchronousLazy<HostnameVerifier>() {
		@Override
		protected HostnameVerifier create() throws Throwable {
			final HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
			return urlProvider.getCertificateFingerprint().length == 0
				? defaultHostnameVerifier
				: new AdditionalHostnameVerifier(new URL(urlProvider.getBaseUrl()).getHost(), defaultHostnameVerifier);
		}
	};

	public ConnectionProvider(IUrlProvider urlProvider) {
		this.urlProvider = urlProvider;
	}

	@Override
	public HttpURLConnection getConnection(String... params) throws IOException {
		if (urlProvider == null) return null;

		final URL url = new URL(urlProvider.getUrl(params));
		final String authCode = urlProvider.getAuthCode();

		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(180000);

		if (connection instanceof HttpsURLConnection) {
			final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
			httpsConnection.setSSLSocketFactory(lazySslSocketFactory.getObject());
			httpsConnection.setHostnameVerifier(lazyHostnameVerifier.getObject());
		}

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
	}

	@Override
	public OkHttpClient getClient() {
		return lazyOkHttpClient.getObject();
	}

	@Override
	public Promise<Response> promiseResponse(String... params) {
		if (urlProvider == null) return null;

		final URL url;
		try {
			url = new URL(urlProvider.getUrl(params));
		} catch (MalformedURLException e) {
			return new Promise<>(e);
		}

		final Request request = new Request.Builder().url(url).build();
		return new HttpPromisedResponse(lazyOkHttpClient.getObject().newCall(request));
	}

	@Override
	public Response getResponse(String... params) throws IOException {
		if (urlProvider == null) return null;

		final URL url = new URL(urlProvider.getUrl(params));

		final Request request = new Request.Builder().url(url).build();
		return lazyOkHttpClient.getObject().newCall(request).execute();
	}

	@Override
	public X509TrustManager getTrustManager() {
		return lazyTrustManager.getObject();
	}

	@Override
	public SSLSocketFactory getSslSocketFactory() {
		return lazySslSocketFactory.getObject();
	}

	public IUrlProvider getUrlProvider() {
		return urlProvider;
	}

	@Override
	public HostnameVerifier getHostnameVerifier() {
		return lazyHostnameVerifier.getObject();
	}

}
