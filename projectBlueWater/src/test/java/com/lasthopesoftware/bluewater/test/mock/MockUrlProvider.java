package com.lasthopesoftware.bluewater.test.mock;

import com.lasthopesoftware.bluewater.servers.connection.url.IUrlProvider;

/**
 * Created by david on 2/18/16.
 */
public class MockUrlProvider implements IUrlProvider {
	@Override
	public String getUrl(String... params) {
		return null;
	}

	@Override
	public String getBaseUrl() {
		return "mock";
	}
}
