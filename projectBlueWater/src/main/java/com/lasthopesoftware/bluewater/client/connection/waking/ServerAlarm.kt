package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.network.LookupActiveNetwork
import com.namehillsoftware.handoff.promises.Promise

private const val broadcastAddress = "255.255.255.255"

class ServerAlarm(
    private val serverLookup: LookupServers,
    private val activeNetwork: LookupActiveNetwork,
    private val server: PokeServer
) : WakeLibraryServer {
	override fun awakeLibraryServer(libraryId: LibraryId): Promise<Unit> = Promise.Proxy { cp ->
		serverLookup.promiseServerInformation(libraryId)
			.also(cp::doCancel)
			.eventually { serverInfo ->
				val addresses = serverInfo?.remoteHosts?.let { serverInfo.localHosts.plus(it) } ?: emptySet()

				val broadcastAddresses = mutableSetOf<String>()
				if (activeNetwork.isLocalNetworkActive) {
					broadcastAddresses.add(broadcastAddress)
					activeNetwork.activeNetworkInterface
						?.interfaceAddresses
						?.mapNotNull { a -> a.broadcast?.hostAddress }
						?.also { broadcastAddresses.addAll(it) }
				}

				val macAddresses = serverInfo?.macAddresses ?: emptyList()
				val machineAddresses = addresses
					.union(broadcastAddresses)
					.flatMap { ip -> macAddresses.map { m -> MachineAddress(ip, m) } }

				if (cp.isCancelled) Promise.empty()
				else Promise.whenAll(machineAddresses.map(server::promiseWakeSignal)).also(cp::doCancel)
			}
			.unitResponse()
	}
}
