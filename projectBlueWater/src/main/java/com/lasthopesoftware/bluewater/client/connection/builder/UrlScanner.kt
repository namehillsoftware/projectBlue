package com.lasthopesoftware.bluewater.client.connection.builder

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.JRiverConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.strings.EncodeToBase64
import com.namehillsoftware.handoff.promises.Promise
import java.util.LinkedList

class UrlScanner(
	private val base64: EncodeToBase64,
	private val serverLookup: LookupServers,
	private val connectionSettingsLookup: LookupConnectionSettings,
	private val okHttpClients: ProvideOkHttpClients
) : BuildUrlProviders {

	override fun promiseBuiltUrlProvider(libraryId: LibraryId): Promise<ServerConnection?> =
		connectionSettingsLookup.lookupConnectionSettings(libraryId).eventually { connectionSettings ->
			connectionSettings
				?.let { settings -> promiseBuiltUrlProvider(libraryId, settings) }
				?: Promise(MissingConnectionSettingsException(libraryId))
		}

	private fun promiseBuiltUrlProvider(libraryId: LibraryId, settings: ConnectionSettings): Promise<ServerConnection?> = Promise.Proxy { cp ->
		val authKey =
			if (settings.isUserCredentialsValid()) base64.encodeString(settings.userName + ":" + settings.password)
			else null

		if (cp.isCancelled) Promise.empty()
		else serverLookup
			.promiseServerInformation(libraryId)
			.also(cp::doCancel)
			.eventually {
				it?.let { (httpPort, httpsPort, remoteIp, localIps, _, certificateFingerprint) ->
					val serverConnections = LinkedList<ServerConnection>()

					fun testUrls(): Promise<ServerConnection?> {
						if (cp.isCancelled) return Promise.empty()
						val serverConnection = serverConnections.poll() ?: return Promise.empty()
						return JRiverConnectionProvider(serverConnection, okHttpClients)
							.promiseIsConnectionPossible()
							.also(cp::doCancel)
							.eventually { result -> if (result) Promise(serverConnection) else testUrls() }
					}

					if (!settings.isLocalOnly) {
						if (httpsPort != null) {
							serverConnections.offer(
								ServerConnection(
									authKey,
									remoteIp,
									httpsPort,
									certificateFingerprint
								)
							)
						}

						if (httpPort != null) {
							serverConnections.offer(
								ServerConnection(
									authKey,
									remoteIp,
									httpPort,
								)
							)
						}
					}

					if (httpPort != null) {
						for (ip in localIps) {
							serverConnections.offer(
								ServerConnection(
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
