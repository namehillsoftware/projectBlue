package com.lasthopesoftware.bluewater.client.connection.url;

import com.lasthopesoftware.bluewater.shared.IoCommon;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class MediaServerUrlProvider implements IUrlProvider {

	private final URL baseUrl;
	private final String authCode;
	private final byte[] certificateFingerprint;

	public MediaServerUrlProvider(String authCode, String ipAddress, int port, byte[] certificateFingerprint) throws MalformedURLException {
		this(authCode, new URL(IoCommon.httpsUriScheme, ipAddress, port, ""), certificateFingerprint);
	}

	public MediaServerUrlProvider(String authCode, String ipAddress, int port) throws MalformedURLException {
		this(authCode, new URL(IoCommon.httpUriScheme, ipAddress, port, ""));
	}

	public MediaServerUrlProvider(String authCode, URL baseUrl) throws MalformedURLException {
		this(authCode, baseUrl, new byte[0]);
	}

	private MediaServerUrlProvider(String authCode, URL baseUrl, byte[] certificateFingerprint) throws MalformedURLException {
		this.authCode = authCode;
		this.baseUrl = new URL(baseUrl, "/MCWS/v1/");
		this.certificateFingerprint = certificateFingerprint;
	}

	public String getAuthCode() {
		return authCode;
	}

	@Override
	public byte[] getCertificateFingerprint() {
		return certificateFingerprint;
	}

	public String getBaseUrl() {
		return baseUrl.toString();
	}

	public String getUrl(String... params) {
		// Add base url
		if (params.length == 0) return getBaseUrl();

		final StringBuilder urlBuilder = new StringBuilder(getBaseUrl());

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
