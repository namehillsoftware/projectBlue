package com.lasthopesoftware.bluewater.servers.connection;

public class AccessConfiguration {
	private final String baseUrl;
	private final String authCode;

	public AccessConfiguration(String authCode, String ipAddress, int port) {
		this.authCode = authCode;

		baseUrl = "http://" + ipAddress + ":" + String.valueOf(port) + "/MCWS/v1/";
	}

	public String getAuthCode() {
		return authCode;
	}

	public String getBaseUrl() {
		return baseUrl;
	}
}
