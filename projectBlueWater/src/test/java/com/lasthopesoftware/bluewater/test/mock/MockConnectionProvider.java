package com.lasthopesoftware.bluewater.test.mock;

import com.lasthopesoftware.bluewater.servers.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.servers.connection.url.IUrlProvider;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by david on 2/18/16.
 */
public class MockConnectionProvider implements IConnectionProvider {
	@Override
	public HttpURLConnection getConnection(String... params) throws IOException {
		return null;
	}

	@Override
	public IUrlProvider getUrlProvider() {
		return new MockUrlProvider();
	}
}
