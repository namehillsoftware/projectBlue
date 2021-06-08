package com.lasthopesoftware.bluewater.client.connection.builder.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.resources.network.LookupActiveNetwork
import com.namehillsoftware.handoff.promises.Promise

class LiveUrlProvider(private val activeNetworkLookup: LookupActiveNetwork,	private val urlProviderBuilder: BuildUrlProviders) : ProvideLiveUrl {
	override fun promiseLiveUrl(libraryId: LibraryId): Promise<IUrlProvider?> =
		if (activeNetworkLookup.activeNetworkInfo != null) urlProviderBuilder.promiseBuiltUrlProvider(libraryId)
		else Promise.empty()
}
