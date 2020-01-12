package com.lasthopesoftware.bluewater.client.connection.builder;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.namehillsoftware.handoff.promises.Promise;

public interface BuildUrlProviders {
	Promise<IUrlProvider> promiseBuiltUrlProvider(final Library library);
}
