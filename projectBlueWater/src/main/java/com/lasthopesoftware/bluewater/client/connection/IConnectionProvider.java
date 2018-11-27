package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;

public interface IConnectionProvider {
	HttpURLConnection getConnection(String... params) throws IOException;
	X509TrustManager getTrustManager();
	SSLSocketFactory getSslSocketFactory();
	IUrlProvider getUrlProvider();
	HostnameVerifier getHostnameVerifier();
}
