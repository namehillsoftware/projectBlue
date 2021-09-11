package com.lasthopesoftware.bluewater.client.connection.builder

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.bluewater.client.connection.url.MediaServerUrlProvider
import com.lasthopesoftware.resources.strings.EncodeToBase64
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import com.namehillsoftware.handoff.promises.propagation.RejectionProxy
import com.namehillsoftware.handoff.promises.propagation.ResolutionProxy
import java.net.URL
import java.util.*

class UrlScanner(
	private val base64: EncodeToBase64,
	private val connectionTester: TestConnections,
	private val serverLookup: LookupServers,
	private val connectionSettingsLookup: LookupConnectionSettings,
	private val okHttpClients: ProvideOkHttpClients
) : BuildUrlProviders {

	override fun promiseBuiltUrlProvider(libraryId: LibraryId): Promise<IUrlProvider?> =
		connectionSettingsLookup.lookupConnectionSettings(libraryId).eventually { connectionSettings ->
			connectionSettings
				?.let { settings -> promiseBuiltUrlProvider(libraryId, settings) }
				?: Promise(MissingConnectionSettingsException(libraryId))
		}

	private fun promiseBuiltUrlProvider(libraryId: LibraryId, settings: ConnectionSettings): Promise<IUrlProvider?> = Promise { m ->
		val cancellationProxy = CancellationProxy()
		m.cancellationRequested(cancellationProxy)

		val authKey =
			if (settings.isUserCredentialsValid()) base64.encodeString(settings.userName + ":" + settings.password)
			else null

		if (cancellationProxy.isCancelled) {
			m.sendResolution(null)
			return@Promise
		}

		val mediaServerUrlProvider = MediaServerUrlProvider(authKey, parseAccessCode(settings.accessCode))
		connectionTester
			.promiseIsConnectionPossible(ConnectionProvider(mediaServerUrlProvider, okHttpClients))
			.also(cancellationProxy::doCancel)
			.eventually { isValid ->
				if (isValid) Promise(mediaServerUrlProvider)
				else serverLookup
					.promiseServerInformation(libraryId)
					.eventually {
						it?.let { (httpPort, httpsPort, remoteIp, localIps, _, certificateFingerprint) ->
							val mediaServerUrlProvidersQueue = LinkedList<IUrlProvider>()

							fun testUrls(): Promise<IUrlProvider?> {
								if (cancellationProxy.isCancelled) return Promise.empty()
								val urlProvider = mediaServerUrlProvidersQueue.poll() ?: return Promise.empty()
								return connectionTester
									.promiseIsConnectionPossible(ConnectionProvider(urlProvider, okHttpClients))
									.also(cancellationProxy::doCancel)
									.eventually { result -> if (result) Promise(urlProvider) else testUrls() }
							}

							if (!settings.isLocalOnly) {
								if (httpsPort != null) {
									mediaServerUrlProvidersQueue.offer(
										MediaServerUrlProvider(
											authKey,
											remoteIp,
											httpsPort,
											certificateFingerprint?.decodeHex() ?: ByteArray(0)
										)
									)
								}

								mediaServerUrlProvidersQueue.offer(
									MediaServerUrlProvider(
										authKey,
										remoteIp,
										httpPort
									)
								)
							}

							for (ip in localIps) {
								mediaServerUrlProvidersQueue.offer(
									MediaServerUrlProvider(
										authKey,
										ip,
										httpPort
									)
								)
							}

							testUrls()
						} ?: Promise.empty()
					}
			}
			.then(ResolutionProxy(m), RejectionProxy(m))
	}

	companion object {
		private fun ConnectionSettings.isUserCredentialsValid(): Boolean =
				userName != null && userName.isNotEmpty() && password != null && password.isNotEmpty()

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

		private fun isPositiveInteger(string: String): Boolean = string.toCharArray().all(Character::isDigit)

		private fun String.decodeHex(): ByteArray {
			val data = this.toCharArray()
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
