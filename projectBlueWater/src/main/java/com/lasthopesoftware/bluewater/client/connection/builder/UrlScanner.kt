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
import com.lasthopesoftware.bluewater.shared.promises.extensions.CancellableProxyPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.strings.EncodeToBase64
import com.lasthopesoftware.resources.uri.IoCommon
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL
import java.util.LinkedList

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

	@OptIn(ExperimentalStdlibApi::class)
	private fun promiseBuiltUrlProvider(libraryId: LibraryId, settings: ConnectionSettings): Promise<IUrlProvider?> = CancellableProxyPromise { cp ->
		val authKey =
			if (settings.isUserCredentialsValid()) base64.encodeString(settings.userName + ":" + settings.password)
			else null

		val mediaServerUrlProvider = MediaServerUrlProvider(
			authKey, parseAccessCode(settings), settings.sslCertificateFingerprint)

		if (cp.isCancelled) Promise.empty()
		else connectionTester
			.promiseIsConnectionPossible(ConnectionProvider(mediaServerUrlProvider, okHttpClients))
			.also(cp::doCancel)
			.eventually { isValid ->
				if (isValid) Promise(mediaServerUrlProvider)
				else serverLookup
					.promiseServerInformation(libraryId)
					.also(cp::doCancel)
					.eventually {
						it?.let { (httpPort, httpsPort, remoteIp, localIps, _, certificateFingerprint) ->
							val mediaServerUrlProvidersQueue = LinkedList<IUrlProvider>()

							fun testUrls(): Promise<IUrlProvider?> {
								if (cp.isCancelled) return Promise.empty()
								val urlProvider = mediaServerUrlProvidersQueue.poll() ?: return Promise.empty()
								return connectionTester
									.promiseIsConnectionPossible(ConnectionProvider(urlProvider, okHttpClients))
									.also(cp::doCancel)
									.eventually { result -> if (result) Promise(urlProvider) else testUrls() }
							}

							if (!settings.isLocalOnly) {
								if (httpsPort != null) {
									mediaServerUrlProvidersQueue.offer(
										MediaServerUrlProvider(
											authKey,
											remoteIp,
											httpsPort,
											certificateFingerprint?.hexToByteArray() ?: ByteArray(0)
										)
									)
								}

								mediaServerUrlProvidersQueue.offer(
									MediaServerUrlProvider(
										authKey,
										remoteIp,
										httpPort,
									)
								)
							}

							for (ip in localIps) {
								mediaServerUrlProvidersQueue.offer(
									MediaServerUrlProvider(
										authKey,
										ip,
										httpPort,
									)
								)
							}

							testUrls()
						}.keepPromise()
					}
			}
	}

	companion object {
		private fun ConnectionSettings.isUserCredentialsValid(): Boolean =
				!userName.isNullOrEmpty() && !password.isNullOrEmpty()

		private fun parseAccessCode(connectionSettings: ConnectionSettings): URL = with(connectionSettings) {
			var url = accessCode

			val scheme = when {
				url.startsWith("http://") -> {
					url = url.replaceFirst("http://", "")
					IoCommon.httpUriScheme
				}
				url.startsWith("https://") -> {
					url = url.replaceFirst("https://", "")
					IoCommon.httpsUriScheme
				}
				sslCertificateFingerprint.any() -> IoCommon.httpsUriScheme
				else -> IoCommon.httpUriScheme
			}

			val urlParts = url.split(":", limit = 2)
			val port = if (urlParts.size > 1 && isPositiveInteger(urlParts[1])) urlParts[1].toInt() else 80
			URL(scheme, urlParts[0], port, "")
		}

		private fun isPositiveInteger(string: String): Boolean = string.toCharArray().all(Character::isDigit)
	}
}
