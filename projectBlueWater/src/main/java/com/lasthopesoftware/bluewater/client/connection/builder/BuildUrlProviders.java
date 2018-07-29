package com.lasthopesoftware.bluewater.client.connection.builder;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public interface BuildUrlProviders {
	Promise<IUrlProvider> promiseBuiltUrlProvider(final Library library);
}
