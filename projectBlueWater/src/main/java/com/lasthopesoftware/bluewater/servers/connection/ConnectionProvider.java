package com.lasthopesoftware.bluewater.servers.connection;

import com.lasthopesoftware.bluewater.servers.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.servers.connection.url.MediaServerUrlProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionProvider {

	private final MediaServerUrlProvider urlProvider;

	public ConnectionProvider(MediaServerUrlProvider urlProvider) {
		this.urlProvider = urlProvider;
	}

	public HttpURLConnection getConnection(String... params) throws IOException {
		if (urlProvider == null) return null;

		final URL url = new URL(urlProvider.getUrl(params));
		final String authCode = urlProvider.getAuthCode();

		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(180000);

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
	}

	public IUrlProvider getUrlProvider() {
		return urlProvider;
	}
}
