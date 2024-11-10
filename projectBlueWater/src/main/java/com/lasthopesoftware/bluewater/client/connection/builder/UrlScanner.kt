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
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.strings.EncodeToBase64
import com.namehillsoftware.handoff.promises.Promise
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

	private fun promiseBuiltUrlProvider(libraryId: LibraryId, settings: ConnectionSettings): Promise<IUrlProvider?> = Promise.Proxy { cp ->
		val authKey =
			if (settings.isUserCredentialsValid()) base64.encodeString(settings.userName + ":" + settings.password)
			else null

		if (cp.isCancelled) Promise.empty()
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
									certificateFingerprint
								)
							)
						}

						if (httpPort != null) {
							mediaServerUrlProvidersQueue.offer(
								MediaServerUrlProvider(
									authKey,
									remoteIp,
									httpPort,
								)
							)
						}
					}

					if (httpPort != null) {
						for (ip in localIps) {
							mediaServerUrlProvidersQueue.offer(
								MediaServerUrlProvider(
									authKey,
									ip,
									httpPort,
								)
							)
						}
					}

					testUrls()
				}.keepPromise()
			}
	}
}
