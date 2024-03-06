package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Context
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

class SelectedConnection(
	private val sendApplicationMessages: SendApplicationMessages,
	private val selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val libraryConnections: ManageConnectionSessions
) {

	fun promiseTestedSessionConnection(): Promise<IConnectionProvider?> =
		selectedLibraryIdentifierProvider.promiseSelectedLibraryId().eventually { selectedLibraryId ->
			selectedLibraryId
				?.let {
					libraryConnections.promiseTestedLibraryConnection(it)
						.apply {
							progress.then { p ->
								if (p != BuildingConnectionStatus.BuildingConnectionComplete) {
									if (p != null) doStateChange(p)
									updates(::doStateChange)
								}
							}
						}
				}
				.keepPromise()
		}

	fun promiseSessionConnection(): Promise<IConnectionProvider?> =
		selectedLibraryIdentifierProvider.promiseSelectedLibraryId().eventually { selectedLibraryId ->
			selectedLibraryId
				?.let {
					libraryConnections
						.promiseLibraryConnection(it)
						.apply {
							progress.then { p ->
								if (p != BuildingConnectionStatus.BuildingConnectionComplete) {
									if (p != null) doStateChange(p)
									updates(::doStateChange)
								}
							}
						}
				}
				.keepPromise()
		}

	private fun doStateChange(status: BuildingConnectionStatus) {
		sendApplicationMessages.sendMessage(BuildSessionConnectionBroadcast(status))
		if (status === BuildingConnectionStatus.BuildingConnectionComplete) logger.info("Session started.")
	}

	class BuildSessionConnectionBroadcast(val buildingConnectionStatus: BuildingConnectionStatus) : ApplicationMessage

	companion object {
		private val logger by lazy { LoggerFactory.getLogger(SelectedConnection::class.java) }

		fun getInstance(context: Context): SelectedConnection =
			SelectedConnection(
                getApplicationMessageBus(),
				context.getCachedSelectedLibraryIdProvider(),
				ConnectionSessionManager.get(context)
			)
	}
}
