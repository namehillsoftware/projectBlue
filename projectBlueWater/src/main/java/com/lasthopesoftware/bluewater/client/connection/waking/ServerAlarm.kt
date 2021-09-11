package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy

class ServerAlarm(private val serverLookup: LookupServers, private val server: PokeServer, private val alarmConfiguration: AlarmConfiguration) : WakeLibraryServer {
	override fun awakeLibraryServer(libraryId: LibraryId): Promise<Unit> =
		Promise { m ->
			val cancellationProxy = CancellationProxy()
			m.cancellationRequested(cancellationProxy)
			serverLookup.promiseServerInformation(libraryId)
				.also(cancellationProxy::doCancel)
				.eventually { serverInfo ->
					val ips = serverInfo?.remoteIp?.let { serverInfo.localIps.plus(it) } ?: emptyList()
					val macAddresses = serverInfo?.macAddresses ?: emptyList()
					val addresses = ips.flatMap { ip -> macAddresses.map { m -> MachineAddress(ip, m) } }

					if (cancellationProxy.isCancelled) Promise.empty()
					else Promise.whenAll(
						addresses.map {
							server.promiseWakeSignal(
								it,
								alarmConfiguration.timesToWake,
								alarmConfiguration.timesBetweenWaking)
						})
						.also(cancellationProxy::doCancel)
				}
				.then({ m.sendResolution(Unit) }, m::sendRejection)
		}
}
