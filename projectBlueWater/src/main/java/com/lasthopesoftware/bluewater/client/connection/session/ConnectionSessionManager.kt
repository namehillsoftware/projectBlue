package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Context
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.promises.extensions.keepPromise
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
							it.then({ c ->
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
							doCancel(it)
							proxyProgress(it)
						}
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
							proxyProgress(it)
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
		fun get(context: Context): ManageConnectionSessions = context.applicationDependencies.connectionSessions
	}
}
