package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;

public interface IConnectionProvider {
	Promise<Response> promiseResponse(String... params);
	Response getResponse(String... params) throws IOException;
	X509TrustManager getTrustManager();
	SSLSocketFactory getSslSocketFactory();
	IUrlProvider getUrlProvider();
	HostnameVerifier getHostnameVerifier();
}
