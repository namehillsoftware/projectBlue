package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class ServerAlarm(private val serverLookup: LookupServers, private val server: PokeServer, private val alarmConfiguration: AlarmConfiguration) : WakeLibraryServer {
	override fun awakeLibraryServer(libraryId: LibraryId): Promise<Unit> =
		CancellableProxyPromise { cp ->
			serverLookup.promiseServerInformation(libraryId)
				.also(cp::doCancel)
				.eventually { serverInfo ->
					val ips = serverInfo?.remoteIp?.let { serverInfo.localIps.plus(it) } ?: emptyList()
					val macAddresses = serverInfo?.macAddresses ?: emptyList()
					val addresses = ips.flatMap { ip -> macAddresses.map { m -> MachineAddress(ip, m) } }

					if (cp.isCancelled) Promise.empty()
					else Promise.whenAll(
						addresses.map {
							server.promiseWakeSignal(
								it,
								alarmConfiguration.timesToWake,
								alarmConfiguration.timesBetweenWaking)
						})
						.also(cp::doCancel)
				}
				.unitResponse()
		}
}
