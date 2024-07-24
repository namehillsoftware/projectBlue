package com.lasthopesoftware.bluewater.client.connection.builder.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

class ServerLookup(private val serverInfoXmlRequest: RequestServerInfoXml) : LookupServers {


	companion object {
		private const val ipElement = "ip"
		private const val localIpListElement = "localiplist"
		private const val portElement = "port"
		private const val httpsPortElementTag = "https_port"
		private const val certificateFingerprintElementTag = "certificate_fingerprint"
		private const val statusAttribute = "Status"
		private const val errorStatusValue = "Error"
		private const val msgElement = "msg"
		private const val macAddressElement = "macaddresslist"
	}

	override fun promiseServerInformation(libraryId: LibraryId): Promise<ServerInfo?> = Promise.Proxy { cp ->
		serverInfoXmlRequest.promiseServerInfoXml(libraryId)
			.also(cp::doCancel)
			.then { xml ->
				if (xml == null || cp.isCancelled) return@then null

				val response = xml.firstElementChild() ?: return@then null
				if (response.hasAttr(statusAttribute) && errorStatusValue == response.attr(statusAttribute)) {
					val element = response.getElementsByTag(msgElement).firstOrNull()
					if (element != null) throw ServerDiscoveryException(
						libraryId,
						element.text()
					)
					throw ServerDiscoveryException(libraryId)
				}

				val remoteIp = response.getElementsByTag(ipElement).single()
				val localIps = response.getElementsByTag(localIpListElement).single()
				val portXml = response.getElementsByTag(portElement).single()
				val macAddresses = response.getElementsByTag(macAddressElement).single()

				var serverInfo = ServerInfo(
					remoteIp = remoteIp.wholeOwnText(),
					localIps = listOf(*localIps.wholeOwnText().split(",").toTypedArray()),
					httpPort = portXml.wholeOwnText().toInt(),
					macAddresses = listOf(*macAddresses.wholeOwnText().trim().split(",").toTypedArray())
				)

				val httpsPortElement = response.getElementsByTag(httpsPortElementTag).firstOrNull()
				if (httpsPortElement != null)
					serverInfo = serverInfo.copy(httpsPort = httpsPortElement.wholeOwnText().toInt())

				val certificateFingerprintElement = response.getElementsByTag(certificateFingerprintElementTag).firstOrNull()
				if (certificateFingerprintElement != null)
					serverInfo =
						serverInfo.copy(certificateFingerprint = certificateFingerprintElement.wholeOwnText())

				serverInfo
			}
	}
}
