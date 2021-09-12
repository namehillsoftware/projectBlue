package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.namehillsoftware.handoff.promises.Promise

class ServerLookup(private val serverInfoXmlRequest: RequestServerInfoXml) : LookupServers {


	companion object {
		private const val ipElement = "ip"
		private const val localIpListElement = "localiplist"
		private const val portElement = "port"
		private const val httpsPortElement = "https_port"
		private const val certificateFingerprintElement = "certificate_fingerprint"
		private const val statusAttribute = "Status"
		private const val errorStatusValue = "Error"
		private const val msgElement = "msg"
		private const val macAddressElement = "macaddresslist"
	}

	override fun promiseServerInformation(libraryId: LibraryId): Promise<ServerInfo?> = CancellableProxyPromise { cp ->
		serverInfoXmlRequest.promiseServerInfoXml(libraryId)
			.also(cp::doCancel)
			.then { xml ->
				if (xml == null || cp.isCancelled) return@then null

				if (xml.containsAttribute(statusAttribute) && errorStatusValue == xml.getAttribute(statusAttribute)) {
					if (xml.contains(msgElement)) throw ServerDiscoveryException(
						libraryId,
						xml.getUnique(msgElement).value
					)
					throw ServerDiscoveryException(libraryId)
				}

				val remoteIp = xml.getUnique(ipElement)
				val localIps = xml.getUnique(localIpListElement)
				val portXml = xml.getUnique(portElement)
				val macAddresses = xml.getUnique(macAddressElement)

				var serverInfo = ServerInfo(
					remoteIp = remoteIp.value,
					localIps = listOf(*localIps.value.split(",").toTypedArray()),
					httpPort = portXml.value.toInt(),
					macAddresses = listOf(*macAddresses.value.trim().split(",").toTypedArray())
				)

				if (xml.contains(httpsPortElement))
					serverInfo = serverInfo.copy(httpsPort = xml.getUnique(httpsPortElement).value.toInt())

				if (xml.contains(certificateFingerprintElement))
					serverInfo =
						serverInfo.copy(certificateFingerprint = xml.getUnique(certificateFingerprintElement).value)

				serverInfo
			}
	}
}
