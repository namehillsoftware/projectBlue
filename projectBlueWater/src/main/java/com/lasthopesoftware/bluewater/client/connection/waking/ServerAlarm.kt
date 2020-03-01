package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.namehillsoftware.handoff.promises.Promise

class ServerAlarm(private val serverLookup: LookupServers, private val server: PokeServer, private val alarmConfiguration: AlarmConfiguration) : WakeLibraryServer {
	override fun awakeLibraryServer(libraryId: LibraryId): Promise<Unit> {
		return serverLookup.promiseServerInformation(libraryId)
			.eventually { serverInfo ->
				val ips = serverInfo?.remoteIp?.let { serverInfo.localIps.plus(it) } ?: emptyList()
				val macAddresses = serverInfo?.macAddresses ?: emptyList()
				val addresses = ips.flatMap { ip -> macAddresses.map { m -> MachineAddress(ip, m) } }

				Promise.whenAll(
					addresses.map {
						server.promiseWakeSignal(it, alarmConfiguration.timesToWake, alarmConfiguration.timesBetweenWaking)
					})
			}
			.then { Unit }
	}
}
