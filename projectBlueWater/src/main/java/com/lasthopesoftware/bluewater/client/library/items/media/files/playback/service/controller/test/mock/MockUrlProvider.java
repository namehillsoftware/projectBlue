package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller.test.mock;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;

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
