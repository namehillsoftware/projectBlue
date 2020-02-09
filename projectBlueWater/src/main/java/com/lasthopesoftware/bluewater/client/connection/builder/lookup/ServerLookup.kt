package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
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

	override fun promiseServerInformation(library: Library): Promise<ServerInfo?> {
		return serverInfoXmlRequest.promiseServerInfoXml(library)
			.then {
				if (it == null) return@then null

				if (it.containsAttribute(statusAttribute) && errorStatusValue == it.getAttribute(statusAttribute)) {
					if (it.contains(msgElement)) throw ServerDiscoveryException(library, it.getUnique(msgElement).value)
					throw ServerDiscoveryException(library)
				}

				val remoteIp = it.getUnique(ipElement)
				val localIps = it.getUnique(localIpListElement)
				val portXml = it.getUnique(portElement)
				val macAddresses = it.getUnique(macAddressElement)

				var serverInfo = ServerInfo(
					remoteIp = remoteIp.value,
					localIps = listOf(*localIps.value.split(",").toTypedArray()),
					httpPort = portXml.value.toInt(),
					macAddresses = listOf(*macAddresses.value.split(",").toTypedArray()))

				if (it.contains(httpsPortElement))
					serverInfo = serverInfo.copy(httpsPort = it.getUnique(httpsPortElement).value.toInt())

				if (it.contains(certificateFingerprintElement))
					serverInfo = serverInfo.copy(certificateFingerprint = it.getUnique(certificateFingerprintElement).value)

				serverInfo
			}
	}
}
