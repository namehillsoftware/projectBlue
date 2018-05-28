package com.lasthopesoftware.bluewater.client.connection.builder;

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.namehillsoftware.handoff.promises.Promise;

public class UrlScanner implements BuildUrlProviders {
	@Override
	public Promise<IUrlProvider> promiseBuiltUrlProvider(Library library) {
		if (library == null)
			return new Promise<>(new IllegalArgumentException("The library cannot be null"));

		if (library.getAccessCode() == null)
			return new Promise<>(new IllegalArgumentException("The access code cannot be null"));

		return Promise.empty();
	}
}
