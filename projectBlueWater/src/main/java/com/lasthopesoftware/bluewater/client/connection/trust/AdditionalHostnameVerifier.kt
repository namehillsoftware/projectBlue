package com.lasthopesoftware.bluewater.client.connection.trust;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class AdditionalHostnameVerifier implements HostnameVerifier {

	private final String additionalHostname;
	private final HostnameVerifier fallbackHostnameVerifier;

	public AdditionalHostnameVerifier(String additionalHostname, HostnameVerifier fallbackHostnameVerifier) {
		this.additionalHostname = additionalHostname;
		this.fallbackHostnameVerifier = fallbackHostnameVerifier;
	}

	@Override
	public boolean verify(String hostname, SSLSession session) {
		return (hostname != null && hostname.equalsIgnoreCase(additionalHostname))
			|| (fallbackHostnameVerifier != null && fallbackHostnameVerifier.verify(hostname, session));
	}
}
