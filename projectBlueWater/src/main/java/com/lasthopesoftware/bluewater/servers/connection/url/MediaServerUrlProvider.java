package com.lasthopesoftware.bluewater.servers.connection.url;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MediaServerUrlProvider implements IUrlProvider {
	private final String baseUrl;
	private final String authCode;

	public MediaServerUrlProvider(String authCode, String ipAddress, int port) {
		this.authCode = authCode;

		baseUrl = "http://" + ipAddress + ":" + String.valueOf(port) + "/MCWS/v1/";
	}

	public String getAuthCode() {
		return authCode;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getUrl(String... params) {
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
}
