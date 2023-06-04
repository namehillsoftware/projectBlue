package com.lasthopesoftware.bluewater.client.connection.builder.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.resources.network.CheckForActiveNetwork
import com.namehillsoftware.handoff.promises.Promise

class LiveUrlProvider(private val activeNetwork: CheckForActiveNetwork, private val urlProviderBuilder: BuildUrlProviders) : ProvideLiveUrl {
	override fun promiseLiveUrl(libraryId: LibraryId): Promise<IUrlProvider?> =
		if (activeNetwork.isNetworkActive) urlProviderBuilder.promiseBuiltUrlProvider(libraryId)
		else Promise.empty()
}
