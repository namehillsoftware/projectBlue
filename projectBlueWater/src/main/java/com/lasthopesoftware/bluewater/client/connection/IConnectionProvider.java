package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;

public interface IConnectionProvider {
	HttpURLConnection getConnection(String... params) throws IOException;
	OkHttpClient getClient();
	Promise<Response> call(String... params);
	X509TrustManager getTrustManager();
	SSLSocketFactory getSslSocketFactory();
	IUrlProvider getUrlProvider();
	HostnameVerifier getHostnameVerifier();
}
