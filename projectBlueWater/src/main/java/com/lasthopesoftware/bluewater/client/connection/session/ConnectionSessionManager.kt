package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.builder.UrlScanner
import com.lasthopesoftware.bluewater.client.connection.builder.live.LiveUrlProvider
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfoXmlRequest
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.okhttp.OkHttpFactory
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsValidation
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm
import com.lasthopesoftware.bluewater.client.connection.waking.ServerWakeSignal
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSessionManager(
	private val connectionTester: TestConnections,
	private val libraryConnections: ProvideLibraryConnections,
	private val holdConnections: HoldPromisedConnections
) : ManageConnectionSessions, ProvideLibraryConnections {

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		holdConnections.setAndGetPromisedConnection(libraryId) { l, promised ->
			object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
				init {
					promised
						?.also {
							doCancel(it)
							proxyUpdates(it)
						}
						?.then({ c ->
							c?.let {
								connectionTester.promiseIsConnectionPossible(it)
									.then({ isPossible ->
										if (isPossible) resolve(it)
										else updateCachedConnection()
									}, {
										updateCachedConnection()
									})
							} ?: updateCachedConnection()
						}, {
							updateCachedConnection()
						})
						?: updateCachedConnection()
				}

				private fun updateCachedConnection() = proxy(libraryConnections.promiseLibraryConnection(l))
			}
		}

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		holdConnections.getPromisedResolvedConnection(libraryId) ?: holdConnections.setAndGetPromisedConnection(libraryId) { l, promised ->
			object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>() {
				init {
					promised
						?.also {
							doCancel(it)
							proxyUpdates(it)
							proxySuccess(it)
						}
						?.excuse { proxy(libraryConnections.promiseLibraryConnection(l)) }
						?: proxy(libraryConnections.promiseLibraryConnection(l))
				}
			}
		}

	override fun removeConnection(libraryId: LibraryId) {
		holdConnections.removeConnection(libraryId)?.cancel()
	}

	override fun promiseIsConnectionActive(libraryId: LibraryId): Promise<Boolean> =
		holdConnections.getPromisedResolvedConnection(libraryId)?.then { c -> c != null }.keepPromise(false)

	companion object Instance {
		private val connectionRepository by lazy { PromisedConnectionsRepository() }

		private val serverWakeSignal by lazy { ServerWakeSignal(PacketSender()) }

		fun Context.buildNewConnectionSessionManager(): ConnectionSessionManager = get(this)

		fun get(context: Context): ConnectionSessionManager {
			val serverLookup = ServerLookup(ServerInfoXmlRequest(LibraryRepository(context), OkHttpFactory))
			val connectionSettingsLookup = ConnectionSettingsLookup(LibraryRepository(context))

			return ConnectionSessionManager(
				ConnectionTester,
				LibraryConnectionProvider(
					ConnectionSettingsValidation,
					connectionSettingsLookup,
					ServerAlarm(serverLookup, serverWakeSignal, AlarmConfiguration.standard),
					LiveUrlProvider(
						ActiveNetworkFinder(context),
						UrlScanner(
							Base64Encoder,
							ConnectionTester,
							serverLookup,
							connectionSettingsLookup,
							OkHttpFactory
						)
					),
					OkHttpFactory
				),
				connectionRepository
			)
		}
	}
}
