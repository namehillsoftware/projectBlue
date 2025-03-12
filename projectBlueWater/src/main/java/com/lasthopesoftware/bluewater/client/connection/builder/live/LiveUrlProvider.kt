package com.lasthopesoftware.bluewater.client.connection.builder.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.resources.network.LookupActiveNetwork
import com.namehillsoftware.handoff.promises.Promise

class LiveUrlProvider(private val activeNetwork: LookupActiveNetwork, private val urlProviderBuilder: BuildUrlProviders) : ProvideLiveUrl {
	override fun promiseLiveUrl(libraryId: LibraryId): Promise<ProvideUrls?> =
		if (activeNetwork.isNetworkActive) urlProviderBuilder.promiseBuiltUrlProvider(libraryId)
		else Promise.empty()
}
