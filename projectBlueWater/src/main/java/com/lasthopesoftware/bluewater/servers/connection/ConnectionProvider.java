package com.lasthopesoftware.bluewater.servers.connection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionProvider {

	private final AccessConfiguration accessConfiguration;

	public ConnectionProvider(AccessConfiguration accessConfiguration) {
		this.accessConfiguration = accessConfiguration;
	}

	public HttpURLConnection getConnection(String... params) throws IOException {
		if (accessConfiguration == null) return null;

		final URL url = new URL(accessConfiguration.buildMediaCenterUrl(params));
		final String authCode = accessConfiguration.getAuthCode();

		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(180000);

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
	}

	public AccessConfiguration getAccessConfiguration() {
		return accessConfiguration;
	}
}
