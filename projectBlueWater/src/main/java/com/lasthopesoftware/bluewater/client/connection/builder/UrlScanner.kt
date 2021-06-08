package com.lasthopesoftware.bluewater.client.connection.builder

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.libraries.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.lasthopesoftware.resources.strings.EncodeToBase64
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL
import java.util.*

class UrlScanner(
	private val base64: EncodeToBase64,
	private val connectionTester: TestConnections,
	private val serverLookup: LookupServers,
	private val connectionSettingsLookup: ConnectionSettingsLookup,
	private val okHttpClients: ProvideOkHttpClients
) : BuildUrlProviders {

	override fun promiseBuiltUrlProvider(libraryId: LibraryId): Promise<IUrlProvider?> =
		connectionSettingsLookup.lookupConnectionSettings(libraryId).eventually { connectionSettings ->
			connectionSettings?.accessCode?.let { accessCode ->
				val authKey =
					if (isUserCredentialsValid(connectionSettings)) base64.encodeString(connectionSettings.userName + ":" + connectionSettings.password)
					else null

				val mediaServerUrlProvider = MediaServerUrlProvider(authKey, parseAccessCode(accessCode))

				connectionTester
					.promiseIsConnectionPossible(ConnectionProvider(mediaServerUrlProvider, okHttpClients))
					.eventually { isValid ->
						if (isValid) Promise(mediaServerUrlProvider)
						else serverLookup
							.promiseServerInformation(libraryId)
							.eventually {
								it?.let { (httpPort, httpsPort, remoteIp, localIps, _, certificateFingerprint) ->
									val mediaServerUrlProvidersQueue = LinkedList<IUrlProvider>()
									if (!connectionSettings.isLocalOnly) {
										if (httpsPort != null) {
											mediaServerUrlProvidersQueue.offer(
												MediaServerUrlProvider(
													authKey,
													remoteIp,
													httpsPort,
													if (certificateFingerprint != null) decodeHex(certificateFingerprint.toCharArray())
													else ByteArray(0)))
										}

										mediaServerUrlProvidersQueue.offer(
											MediaServerUrlProvider(
												authKey,
												remoteIp,
												httpPort))
									}

									for (ip in localIps) {
										mediaServerUrlProvidersQueue.offer(
											MediaServerUrlProvider(
												authKey,
												ip,
												httpPort))
									}

									testUrls(mediaServerUrlProvidersQueue)
								} ?: Promise.empty()
							}
					}
			} ?: Promise(IllegalArgumentException("The access code cannot be null"))
		}

	private fun testUrls(urls: Queue<IUrlProvider>): Promise<IUrlProvider?> {
		val urlProvider = urls.poll() ?: return Promise.empty()
		return connectionTester
			.promiseIsConnectionPossible(ConnectionProvider(urlProvider, okHttpClients))
			.eventually { result -> if (result) Promise(urlProvider) else testUrls(urls) }
	}

	companion object {
		private fun isUserCredentialsValid(connectionSettings: ConnectionSettings): Boolean =
			(connectionSettings.userName?.isNotEmpty() ?: true)
				&& (connectionSettings.password?.isNotEmpty() ?: true)

		private fun parseAccessCode(accessCode: String): URL {
			var url = accessCode
			var scheme = "http"
			if (url.startsWith("http://")) url = url.replaceFirst("http://", "")
			if (url.startsWith("https://")) {
				url = url.replaceFirst("https://", "")
				scheme = "https"
			}
			val urlParts = url.split(":", limit = 2)
			val port = if (urlParts.size > 1 && isPositiveInteger(urlParts[1])) urlParts[1].toInt() else 80
			return URL(scheme, urlParts[0], port, "")
		}

		private fun isPositiveInteger(string: String): Boolean {
			for (c in string.toCharArray()) if (!Character.isDigit(c)) return false
			return true
		}

		private fun decodeHex(data: CharArray): ByteArray {
			val len = data.size
			if (len and 0x01 != 0) {
				return ByteArray(0)
			}
			val out = ByteArray(len shr 1)

			// two characters form the hex value.
			var i = 0
			var j = 0
			while (j < len) {
				var f = Character.digit(data[j], 16) shl 4
				j++
				f = f or Character.digit(data[j], 16)
				j++
				out[i] = (f and 0xFF).toByte()
				i++
			}
			return out
		}
	}
}
