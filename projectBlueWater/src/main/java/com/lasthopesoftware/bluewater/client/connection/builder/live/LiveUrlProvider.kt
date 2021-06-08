package com.lasthopesoftware.bluewater.client.connection.builder.live;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders;
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider;
import com.lasthopesoftware.resources.network.LookupActiveNetwork;
import com.namehillsoftware.handoff.promises.Promise;

public class LiveUrlProvider implements ProvideLiveUrl {

	private final LookupActiveNetwork activeNetworkLookup;
	private final BuildUrlProviders urlProviderBuilder;

	public LiveUrlProvider(LookupActiveNetwork activeNetworkLookup, BuildUrlProviders urlProviderBuilder) {
		this.activeNetworkLookup = activeNetworkLookup;
		this.urlProviderBuilder = urlProviderBuilder;
	}

	@Override
	public Promise<IUrlProvider> promiseLiveUrl(Library library) {
		return activeNetworkLookup.getActiveNetworkInfo() != null
			? urlProviderBuilder.promiseBuiltUrlProvider(library)
			: Promise.empty();
	}
}
