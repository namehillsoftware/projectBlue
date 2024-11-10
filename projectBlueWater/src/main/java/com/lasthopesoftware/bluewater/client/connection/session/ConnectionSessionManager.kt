package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.PacketSender
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
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
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.network.ActiveNetworkFinder
import com.lasthopesoftware.resources.strings.Base64Encoder
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSessionManager(
	private val connectionTester: TestConnections,
	private val libraryConnections: ProvideLibraryConnections,
	private val holdConnections: HoldPromisedConnections,
	private val sendApplicationMessages: SendApplicationMessages,
) : ManageConnectionSessions {

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		holdConnections.setAndGetPromisedConnection(libraryId) { l, promised ->
			object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>() {
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

				private fun updateCachedConnection() = proxy(promiseUpdatedLibraryConnection(promised, l))
			}
		}

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		holdConnections.getPromisedResolvedConnection(libraryId) ?: holdConnections.setAndGetPromisedConnection(libraryId) { l, promised ->
			object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>() {
				init {
					promised
						?.also {
							doCancel(it)
							proxyUpdates(it)
							proxySuccess(it)
						}
						?.excuse { _ -> proxy(promiseUpdatedLibraryConnection(promised, l)) }
						?: proxy(promiseUpdatedLibraryConnection(promised, l))
				}
			}
		}

	override fun removeConnection(libraryId: LibraryId) {
		holdConnections.removeConnection(libraryId)?.cancel()
	}

	override fun promiseIsConnectionActive(libraryId: LibraryId): Promise<Boolean> =
		holdConnections.getPromisedResolvedConnection(libraryId)?.then { c -> c != null }.keepPromise(false)

	private fun promiseUpdatedLibraryConnection(promised: ProgressingPromise<BuildingConnectionStatus, ProvideConnections?>?, libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.apply {
				then(
					{ newConnection ->
						promised
							?.then { oldConnection ->
								if (oldConnection != newConnection) {
									sendApplicationMessages.sendMessage(
										if (newConnection != null) LibraryConnectionChangedMessage(libraryId)
										else ConnectionLostNotification(libraryId)
									)
								}
							}
							?: run {
								if (newConnection != null)
									sendApplicationMessages.sendMessage(LibraryConnectionChangedMessage(libraryId))
							}
					},
					{
						promised?.then { oldConnection ->
							if (oldConnection != null)
								sendApplicationMessages.sendMessage(ConnectionLostNotification(libraryId))
						}
					}
				)
			}

	companion object Instance {
		private val connectionRepository by lazy { PromisedConnectionsRepository() }

		private val serverWakeSignal by lazy { ServerWakeSignal(PacketSender()) }

		fun Context.buildNewConnectionSessionManager(): ConnectionSessionManager = get(this)

		fun get(context: Context): ConnectionSessionManager {
			val connectionSettingsLookup = ConnectionSettingsLookup(LibraryRepository(context))
			val serverLookup = ServerLookup(
				connectionSettingsLookup,
				ServerInfoXmlRequest(LibraryRepository(context), OkHttpFactory),
			)

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
				connectionRepository,
				ApplicationMessageBus.getApplicationMessageBus(),
			)
		}
	}
}
