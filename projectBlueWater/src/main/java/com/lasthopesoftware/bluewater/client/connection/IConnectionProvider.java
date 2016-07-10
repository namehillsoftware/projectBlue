package com.lasthopesoftware.bluewater.client.connection;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by david on 2/18/16.
 */
public interface IConnectionProvider {
	HttpURLConnection getConnection(String... params) throws IOException;
	IUrlProvider getUrlProvider();
}
