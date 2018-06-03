package com.lasthopesoftware.bluewater.client.connection.trust;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

import javax.net.ssl.TrustManager;

public class TrustManagerProvider implements ProvideTrustManager {

	private final IUrlProvider urlProvider;

	public TrustManagerProvider(IUrlProvider urlProvider) {
		this.urlProvider = urlProvider;
	}

	@Override
	public TrustManager getTrustManager() {
		return null;
	}
}
