package com.lasthopesoftware.bluewater.client.connection.builder.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.BuildUrlProviders
import com.lasthopesoftware.resources.network.LookupActiveNetwork
import com.namehillsoftware.handoff.promises.Promise

class LiveServerConnectionProvider(private val activeNetwork: LookupActiveNetwork, private val urlProviderBuilder: BuildUrlProviders) : ProvideLiveServerConnection {
	override fun promiseLiveServerConnection(libraryId: LibraryId): Promise<ServerConnection?> =
		if (activeNetwork.isNetworkActive) urlProviderBuilder.promiseBuiltUrlProvider(libraryId)
		else Promise.empty()
}
