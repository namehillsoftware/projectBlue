package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class ServerAlarm(private val serverLookup: LookupServers, private val server: PokeServer) : WakeLibraryServer {
	override fun awakeLibraryServer(libraryId: LibraryId): Promise<Unit> =
		Promise.Proxy { cp ->
			serverLookup.promiseServerInformation(libraryId)
				.also(cp::doCancel)
				.eventually { serverInfo ->
					val ips = serverInfo?.remoteHost?.let { serverInfo.localIps.plus(it) } ?: emptyList()
					val macAddresses = serverInfo?.macAddresses ?: emptyList()
					val addresses = ips.flatMap { ip -> macAddresses.map { m -> MachineAddress(ip, m) } }

					if (cp.isCancelled) Promise.empty()
					else Promise.whenAll(addresses.map(server::promiseWakeSignal)).also(cp::doCancel)
				}
				.unitResponse()
		}
}
