package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.test.mock;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

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
