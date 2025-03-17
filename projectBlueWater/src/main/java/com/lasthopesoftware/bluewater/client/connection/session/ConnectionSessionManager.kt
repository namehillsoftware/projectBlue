package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Context
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class ConnectionSessionManager(
	private val libraryConnections: ProvideLibraryConnections,
	private val holdConnections: HoldPromisedConnections,
	private val sendApplicationMessages: SendApplicationMessages,
) : ManageConnectionSessions, ProvideRemoteLibraryAccess {

	override fun promiseTestedLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> =
		holdConnections.setAndGetPromisedConnection(libraryId) { l, promised ->
			object : ProgressingPromiseProxy<BuildingConnectionStatus, LiveServerConnection?>() {
				init {
					promised
						?.also {
							it.then({ c ->
								c?.promiseIsConnectionPossible()
									?.then({ isPossible ->
										if (isPossible) resolve(c)
										else updateCachedConnection()
									}, {
										updateCachedConnection()
									})
									?: updateCachedConnection()
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

	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> =
		holdConnections.getPromisedResolvedConnection(libraryId) ?: holdConnections.setAndGetPromisedConnection(libraryId) { l, promised ->
			object : ProgressingPromiseProxy<BuildingConnectionStatus, LiveServerConnection?>() {
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

	override fun promiseLibraryAccess(libraryId: LibraryId): Promise<RemoteLibraryAccess?> {
		TODO("Not yet implemented")
	}

	override fun removeConnection(libraryId: LibraryId) {
		holdConnections.removeConnection(libraryId)?.cancel()
	}

	override fun promiseIsConnectionActive(libraryId: LibraryId): Promise<Boolean> =
		holdConnections.getPromisedResolvedConnection(libraryId)?.then { c -> c != null }.keepPromise(false)

	private fun promiseUpdatedLibraryConnection(promised: ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>?, libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, LiveServerConnection?> =
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
