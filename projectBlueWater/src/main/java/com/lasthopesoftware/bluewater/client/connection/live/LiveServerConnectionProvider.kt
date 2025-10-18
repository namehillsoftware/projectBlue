package com.lasthopesoftware.bluewater.client.connection.live

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.MediaCenterConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.SubsonicConnectionDetails
import com.lasthopesoftware.bluewater.client.connection.lookup.LookupServers
import com.lasthopesoftware.bluewater.client.connection.requests.ProvideHttpPromiseServerClients
import com.lasthopesoftware.bluewater.client.connection.settings.LookupValidConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideServerHttpDataSource
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.network.LookupActiveNetwork
import com.lasthopesoftware.resources.strings.EncodeToBase64
import com.lasthopesoftware.resources.strings.GetStringResources
import com.lasthopesoftware.resources.strings.TranslateJson
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL
import java.util.LinkedList
import java.util.UUID

class LiveServerConnectionProvider(
	private val activeNetwork: LookupActiveNetwork,
	private val base64: EncodeToBase64,
	private val serverLookup: LookupServers,
	private val connectionSettingsLookup: LookupValidConnectionSettings,
	private val httpMediaCenterClients: ProvideHttpPromiseServerClients<MediaCenterConnectionDetails>,
	private val mediaCenterDataSources: ProvideServerHttpDataSource<MediaCenterConnectionDetails>,
	private val httpSubsonicClients: ProvideHttpPromiseServerClients<SubsonicConnectionDetails>,
	private val subsonicDataSources: ProvideServerHttpDataSource<SubsonicConnectionDetails>,
	private val jsonTranslator: TranslateJson,
	private val stringResources: GetStringResources,
) : ProvideLiveServerConnection {
	override fun promiseLiveServerConnection(libraryId: LibraryId): Promise<LiveServerConnection?> =
		if (activeNetwork.isNetworkActive) {
			connectionSettingsLookup
				.promiseConnectionSettings(libraryId)
				.cancelBackEventually { connectionSettings ->
					when (connectionSettings) {
						is MediaCenterConnectionSettings -> promiseTestedServerConnection(libraryId, connectionSettings)
						is SubsonicConnectionSettings -> promiseTestedServerConnection(libraryId, connectionSettings)
						null -> Promise(MissingConnectionSettingsException(libraryId))
					}
				}
		} else Promise.empty()

	private fun promiseTestedServerConnection(libraryId: LibraryId, settings: MediaCenterConnectionSettings): Promise<LiveServerConnection?> = Promise.Proxy { cp ->
		val authKey =
			if (settings.isUserCredentialsValid()) base64.encodeString(settings.userName + ":" + settings.password)
			else null

		if (cp.isCancelled) Promise.empty()
		else serverLookup
			.promiseServerInformation(libraryId)
			.also(cp::doCancel)
			.eventually {
				it?.let { (httpPort, httpsPort, remoteIps, localIps, _, certificateFingerprint) ->
					val mediaCenterConnectionDetails = LinkedList<MediaCenterConnectionDetails>()

					fun testUrls(): Promise<LiveServerConnection?> {
						if (cp.isCancelled) return Promise.empty()
						val serverConnection = mediaCenterConnectionDetails.poll() ?: return Promise.empty()
						val potentialConnection = LiveMediaCenterConnection(
							serverConnection,
							httpMediaCenterClients,
							mediaCenterDataSources,
						)
						return potentialConnection
							.promiseIsConnectionPossible()
							.also(cp::doCancel)
							.eventually { result -> if (result) Promise(potentialConnection) else testUrls() }
					}

					if (!settings.isLocalOnly) {
						if (httpsPort != null) {
							for (ip in remoteIps) {
								mediaCenterConnectionDetails.offer(
									MediaCenterConnectionDetails(
										authKey,
										ip,
										httpsPort,
										certificateFingerprint
									)
								)
							}
						}

						if (httpPort != null) {
							for (ip in remoteIps) {
								mediaCenterConnectionDetails.offer(
									MediaCenterConnectionDetails(
										authKey,
										ip,
										httpPort,
									)
								)
							}
						}
					}

					if (httpPort != null) {
						for (ip in localIps) {
							mediaCenterConnectionDetails.offer(
								MediaCenterConnectionDetails(
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

	private fun promiseTestedServerConnection(libraryId: LibraryId, settings: SubsonicConnectionSettings): Promise<LiveServerConnection?> = Promise.Proxy { cp ->
		if (cp.isCancelled) Promise.empty()
		else serverLookup
			.promiseServerInformation(libraryId)
			.also(cp::doCancel)
			.eventually {
				it?.let { (httpPort, httpsPort, remoteIps, localIps, _, certificateFingerprint) ->
					val salt = UUID.randomUUID().toString()

					val subsonicConnectionDetails = LinkedList<SubsonicConnectionDetails>()

					fun testUrls(): Promise<LiveServerConnection?> {
						if (cp.isCancelled) return Promise.empty()
						val serverConnection = subsonicConnectionDetails.poll() ?: return Promise.empty()
						val potentialConnection = LiveSubsonicConnection(
							serverConnection,
							httpSubsonicClients,
							subsonicDataSources,
							jsonTranslator,
							stringResources,
						)
						return potentialConnection
							.promiseIsConnectionPossible()
							.also(cp::doCancel)
							.eventually { result -> if (result) Promise(potentialConnection) else testUrls() }
					}

					if (httpsPort != null) {
						for (ip in remoteIps) {
							subsonicConnectionDetails.offer(
								SubsonicConnectionDetails(
									URL("https://$ip:$httpsPort"),
									settings.userName,
									settings.password,
									salt,
									certificateFingerprint
								)
							)
						}
					}

					if (httpPort != null) {
						for (ip in remoteIps) {
							subsonicConnectionDetails.offer(
								SubsonicConnectionDetails(
									URL("http://$ip:$httpPort"),
									settings.userName,
									settings.password,
									salt,
									certificateFingerprint
								)
							)
						}

						for (ip in localIps) {
							subsonicConnectionDetails.offer(
								SubsonicConnectionDetails(
									URL("http://$ip:$httpPort"),
									settings.userName,
									settings.password,
									salt,
									certificateFingerprint
								)
							)
						}
					}

					testUrls()
				}.keepPromise()
			}
	}
}
