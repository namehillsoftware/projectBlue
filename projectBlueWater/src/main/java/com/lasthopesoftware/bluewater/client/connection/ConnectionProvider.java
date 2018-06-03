package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class ConnectionProvider implements IConnectionProvider {

	private final IUrlProvider urlProvider;
	private final SSLSocketFactory sslSocketFactory;

	public ConnectionProvider(IUrlProvider urlProvider, SSLSocketFactory sslSocketFactory) {
		this.urlProvider = urlProvider;
		this.sslSocketFactory = sslSocketFactory;
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
			((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
	}

	public IUrlProvider getUrlProvider() {
		return urlProvider;
	}

}
