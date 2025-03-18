package com.lasthopesoftware.bluewater.client.connection.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ServerConnection
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseClients
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.network.LookupActiveNetwork
import com.lasthopesoftware.resources.strings.EncodeToBase64
import com.namehillsoftware.handoff.promises.Promise
import java.util.LinkedList

class LiveServerConnectionProvider(
	private val activeNetwork: LookupActiveNetwork,
	private val base64: EncodeToBase64,
	private val serverLookup: LookupServers,
	private val connectionSettingsLookup: LookupConnectionSettings,
	private val httpClients: ProvideHttpPromiseClients,
	private val okHttpClients: ProvideOkHttpClients,
) : ProvideLiveServerConnection {
	override fun promiseLiveServerConnection(libraryId: LibraryId): Promise<LiveServerConnection?> =
		if (activeNetwork.isNetworkActive) {
			connectionSettingsLookup
				.lookupConnectionSettings(libraryId)
				.cancelBackEventually { connectionSettings ->
					connectionSettings
						?.let { settings -> promiseTestedServerConnection(libraryId, settings) }
						?: Promise(MissingConnectionSettingsException(libraryId))
				}
		} else Promise.empty()

	private fun promiseTestedServerConnection(libraryId: LibraryId, settings: ConnectionSettings): Promise<LiveServerConnection?> = Promise.Proxy { cp ->
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

					fun testUrls(): Promise<LiveServerConnection?> {
						if (cp.isCancelled) return Promise.empty()
						val serverConnection = serverConnections.poll() ?: return Promise.empty()
						val potentialConnection = MediaCenterConnection(serverConnection, httpClients, okHttpClients)
						return potentialConnection
							.promiseIsConnectionPossible()
							.also(cp::doCancel)
							.eventually { result -> if (result) Promise(potentialConnection) else testUrls() }
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
