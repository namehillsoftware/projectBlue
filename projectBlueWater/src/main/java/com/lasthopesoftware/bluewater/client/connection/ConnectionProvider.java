package com.lasthopesoftware.bluewater.client.connection;

import android.net.SSLCertificateSocketFactory;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class ConnectionProvider implements IConnectionProvider {

	private final IUrlProvider urlProvider;

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
			HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
			httpsConn.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(0, null));
			httpsConn.setHostnameVerifier(new AllowAllHostnameVerifier());
		}

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
	}

	public IUrlProvider getUrlProvider() {
		return urlProvider;
	}

}
