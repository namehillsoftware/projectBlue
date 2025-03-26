package com.lasthopesoftware.bluewater.client.connection.lookup

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.LookupValidConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.emptyByteArray
import com.namehillsoftware.handoff.promises.Promise

class ServerLookup(
	private val connectionSettingsLookup: LookupValidConnectionSettings,
	private val serverInfoXmlRequest: RequestMediaCenterServerInfoXml,
) : LookupServers {
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

		private fun MediaCenterConnectionSettings.parseServerInfo(): Pair<Boolean, ServerInfo> {
			var url = accessCode
			var isValidUrl = false

			val isHttps = when {
				url.startsWith("http://", ignoreCase = true) -> {
					isValidUrl = true
					url = url.replaceFirst("http://", "")
					false
				}

				url.startsWith("https://", ignoreCase = true) -> {
					isValidUrl = true
					url = url.replaceFirst("https://", "")
					true
				}

				sslCertificateFingerprint.any() -> true
				else -> false
			}

			val urlParts = url.split(":", limit = 2)
			val hasPort = urlParts.size > 1 && isPositiveInteger(urlParts[1])

			val port = when {
				hasPort -> urlParts[1].toInt()
				isHttps -> 443
				else -> 80
			}

			isValidUrl = isValidUrl or hasPort
			return Pair(
				isValidUrl, ServerInfo(
					remoteHosts = setOf(urlParts[0]),
					localHosts = emptySet(),
					httpPort = port.takeUnless { isHttps },
					httpsPort = port.takeIf { isHttps },
					macAddresses = macAddress?.takeIf { it.isNotEmpty() }?.let(::setOf) ?: emptySet(),
					certificateFingerprint = sslCertificateFingerprint
				)
			)
		}

		private fun SubsonicConnectionSettings.parseServerInfo(): ServerInfo {
			var host = ""
			val isHttps = when {
				url.startsWith("http://", ignoreCase = true) -> {
					host = url.replaceFirst("http://", "")
					false
				}

				url.startsWith("https://", ignoreCase = true) -> {
					host = url.replaceFirst("https://", "")
					true
				}

				sslCertificateFingerprint.any() -> true
				else -> false
			}

			val urlParts = url.split(":", limit = 2)
			val hasPort = urlParts.size > 1 && isPositiveInteger(urlParts[1])

			val port = when {
				hasPort -> urlParts[1].toInt()
				isHttps -> 443
				else -> 80
			}

			return ServerInfo(
				remoteHosts = setOf(host),
				localHosts = emptySet(),
				httpPort = port.takeUnless { isHttps },
				httpsPort = port.takeIf { isHttps },
				macAddresses = macAddress?.takeIf { it.isNotEmpty() }?.let(::setOf) ?: emptySet(),
				certificateFingerprint = sslCertificateFingerprint
			)
		}

		private fun isPositiveInteger(string: String): Boolean = string.toCharArray().all(Character::isDigit)
	}

	@OptIn(ExperimentalStdlibApi::class)
	override fun promiseServerInformation(libraryId: LibraryId): Promise<ServerInfo> = Promise.Proxy { proxy ->
		connectionSettingsLookup
			.promiseConnectionSettings(libraryId)
			.also(proxy::doCancel)
			.eventually { connectionSettings ->
				if (proxy.isCancelled) Promise.empty()
				else when (connectionSettings) {
					is MediaCenterConnectionSettings -> {
						val settingsServerInfo = connectionSettings.parseServerInfo()

						settingsServerInfo
							.takeIf { it.first }
							?.second
							?.toPromise()
							?: serverInfoXmlRequest
								.promiseServerInfoXml(libraryId)
								.also(proxy::doCancel)
								.then { xml ->
									if (xml == null || proxy.isCancelled) return@then null

									val response = xml.firstElementChild() ?: return@then null
									if (errorStatusValue == response.attr(statusAttribute)) {
										val element = response.getElementsByTag(msgElement).firstOrNull()
										if (element != null) throw ServerDiscoveryException(
											libraryId,
											element.text()
										)

										return@then null
									}

									val remoteIp = response.getElementsByTag(ipElement).single()
									val localIps = response.getElementsByTag(localIpListElement).single()
									val portXml = response.getElementsByTag(portElement).single()
									val macAddresses = response.getElementsByTag(macAddressElement).single()

									val localServerInfo = settingsServerInfo.second

									ServerInfo(
										remoteHosts = setOf(remoteIp.wholeOwnText()),
										localHosts = setOf(*localIps.wholeOwnText().split(",").toTypedArray()),
										httpPort = portXml.wholeOwnText().toInt(),
										httpsPort = response.getElementsByTag(httpsPortElementTag).firstOrNull()
											?.wholeOwnText()?.toInt(),
										macAddresses = macAddresses.wholeOwnText().trim().split(",")
											.toSet()
											.union(localServerInfo.macAddresses),
										certificateFingerprint = localServerInfo
											.certificateFingerprint
											.takeIf { it.any() }
											?: response.getElementsByTag(certificateFingerprintElementTag).firstOrNull()
												?.wholeOwnText()?.hexToByteArray()
											?: emptyByteArray
									)
								}
								.then({ remoteServerInfo ->
									remoteServerInfo ?: settingsServerInfo.second
								}, { e ->
									if (proxy.isCancelled) throw e
									settingsServerInfo.second
								})
					}
					is SubsonicConnectionSettings -> connectionSettings.parseServerInfo().toPromise()
					null -> throw ServerDiscoveryException(libraryId)
				}
			}
	}
}
