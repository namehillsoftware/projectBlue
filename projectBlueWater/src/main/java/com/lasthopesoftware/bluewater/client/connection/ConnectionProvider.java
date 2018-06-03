package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.trust.SelfSignedTrustManager;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.lazyj.AbstractSynchronousLazy;
import com.namehillsoftware.lazyj.CreateAndHold;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class ConnectionProvider implements IConnectionProvider {

	private final IUrlProvider urlProvider;

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

			return urlProvider.getCertificateFingerprint() == null
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

		if (connection instanceof HttpsURLConnection)
			((HttpsURLConnection) connection).setSSLSocketFactory(lazySslSocketFactory.getObject());

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
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

}
