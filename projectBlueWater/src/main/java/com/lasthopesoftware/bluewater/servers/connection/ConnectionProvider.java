package com.lasthopesoftware.bluewater.servers.connection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ConnectionProvider {

	private final AccessConfiguration accessConfiguration;

	public ConnectionProvider(AccessConfiguration accessConfiguration) {
		this.accessConfiguration = accessConfiguration;
	}

	public HttpURLConnection getConnection(String... params) throws IOException {
		if (accessConfiguration == null) return null;

		final URL url = new URL(buildMediaCenterUrl(params));
		final String authCode = accessConfiguration.getAuthCode();

		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(180000);

		if (authCode != null && !authCode.isEmpty())
			connection.setRequestProperty("Authorization", "basic " + authCode);

		return connection;
	}

	public String buildMediaCenterUrl(String... params) {
		final String baseUrl = accessConfiguration.getBaseUrl();

		// Add base url
		if (params.length == 0) return baseUrl;

		final StringBuilder urlBuilder = new StringBuilder(baseUrl);

		// Add action
		urlBuilder.append(params[0]);

		// add arguments
		if (params.length > 1) {
			for (int i = 1; i < params.length; i++) {
				urlBuilder.append(i == 1 ? '?' : '&');

				final String param = params[i];

				final int equalityIndex = param.indexOf('=');
				if (equalityIndex < 0) {
					urlBuilder.append(encodeParameter(param));
					continue;
				}

				urlBuilder.append(encodeParameter(param.substring(0, equalityIndex))).append('=').append(encodeParameter(param.substring(equalityIndex + 1)));
			}
		}

		return urlBuilder.toString();
	}

	private static String encodeParameter(String parameter) {
		try {
			return URLEncoder.encode(parameter, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			return parameter;
		}
	}

	public AccessConfiguration getAccessConfiguration() {
		return accessConfiguration;
	}
}
