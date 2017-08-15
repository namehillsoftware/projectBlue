package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface IConnectionProvider {
	HttpURLConnection getConnection(String... params) throws IOException;
	IUrlProvider getUrlProvider();
}
