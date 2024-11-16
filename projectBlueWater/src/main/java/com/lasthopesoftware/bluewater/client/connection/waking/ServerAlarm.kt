package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.promises.extensions.unitResponse
import com.lasthopesoftware.resources.network.CheckForActiveNetwork
import com.namehillsoftware.handoff.promises.Promise

class ServerAlarm(
	private val serverLookup: LookupServers,
	private val activeNetwork: CheckForActiveNetwork,
	private val server: PokeServer
) : WakeLibraryServer {
	override fun awakeLibraryServer(libraryId: LibraryId): Promise<Unit> =
		Promise.Proxy { cp ->
			serverLookup.promiseServerInformation(libraryId)
				.also(cp::doCancel)
				.eventually { serverInfo ->
					val addresses = serverInfo?.remoteHost?.let { serverInfo.localIps.plus(it) } ?: emptySet()

					val broadcastAddresses = activeNetwork.activeNetwork
						?.interfaceAddresses?.mapNotNull { a -> a.broadcast?.hostAddress }
						?: emptyList()

					val macAddresses = serverInfo?.macAddresses ?: emptyList()
					val machineAddresses = addresses.union(broadcastAddresses).flatMap { ip -> macAddresses.map { m -> MachineAddress(ip, m) } }

					if (cp.isCancelled) Promise.empty()
					else Promise.whenAll(machineAddresses.map(server::promiseWakeSignal)).also(cp::doCancel)
				}
				.unitResponse()
		}
}
