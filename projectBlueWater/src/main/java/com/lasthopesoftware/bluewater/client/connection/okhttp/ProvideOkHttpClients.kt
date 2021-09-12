package com.lasthopesoftware.bluewater.client.connection.okhttp;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import okhttp3.OkHttpClient;

public interface ProvideOkHttpClients {
	OkHttpClient getOkHttpClient(IUrlProvider urlProvider);
}
