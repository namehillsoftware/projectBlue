package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedBrowserLibraryIdentifierProvider
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.MessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.SendMessages
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

class SelectedConnection(
	private val localBroadcastManager: SendMessages,
	private val selectedLibraryIdentifierProvider: ProvideSelectedLibraryId,
	private val libraryConnections: ManageConnectionSessions
) {

	fun promiseTestedSessionConnection(): Promise<IConnectionProvider?> =
		selectedLibraryIdentifierProvider.selectedLibraryId.eventually { selectedLibraryId ->
			selectedLibraryId
				?.let {
					libraryConnections.promiseTestedLibraryConnection(selectedLibraryId)
						.also {
							it.progress.then { progress ->
								if (progress != BuildingConnectionStatus.BuildingConnectionComplete) {
									if (progress != null) doStateChange(progress)
									it.updates(::doStateChange)
								}
							}
						}
				}
				.keepPromise()
		}

	fun isSessionConnectionActive(): Promise<Boolean> =
		selectedLibraryIdentifierProvider.selectedLibraryId.then { id ->
			id?.let(libraryConnections::isConnectionActive) ?: false
		}

	fun promiseSessionConnection(): Promise<IConnectionProvider?> =
		selectedLibraryIdentifierProvider.selectedLibraryId.eventually { selectedLibraryId ->
			selectedLibraryId
				?.let {
					libraryConnections
						.promiseLibraryConnection(selectedLibraryId)
						.also {
							it.progress.then { progress ->
								if (progress != BuildingConnectionStatus.BuildingConnectionComplete) {
									if (progress != null) doStateChange(progress)
									it.updates(::doStateChange)
								}
							}
						}
				}
				.keepPromise()
		}

	private fun doStateChange(status: BuildingConnectionStatus) {
		val broadcastIntent = Intent(buildSessionBroadcast)
		broadcastIntent.putExtra(
			buildSessionBroadcastStatus,
			BuildingSessionConnectionStatus.getSessionConnectionStatus(status)
		)
		localBroadcastManager.sendBroadcast(broadcastIntent)
		if (status === BuildingConnectionStatus.BuildingConnectionComplete) logger.info("Session started.")
	}

	object BuildingSessionConnectionStatus {
		const val GettingLibrary = 1
		const val GettingLibraryFailed = 2
		const val SendingWakeSignal = 3
		const val BuildingConnection = 4
		const val BuildingConnectionFailed = 5
		const val BuildingSessionComplete = 6

		@SessionConnectionStatus
		fun getSessionConnectionStatus(connectionStatus: BuildingConnectionStatus): Int =
			when (connectionStatus) {
				BuildingConnectionStatus.GettingLibrary -> GettingLibrary
				BuildingConnectionStatus.SendingWakeSignal -> SendingWakeSignal
				BuildingConnectionStatus.GettingLibraryFailed -> GettingLibraryFailed
				BuildingConnectionStatus.BuildingConnection -> BuildingConnection
				BuildingConnectionStatus.BuildingConnectionFailed -> BuildingConnectionFailed
				BuildingConnectionStatus.BuildingConnectionComplete -> BuildingSessionComplete
			}

		@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
		@IntDef(GettingLibrary, GettingLibraryFailed, SendingWakeSignal, BuildingConnection, BuildingConnectionFailed, BuildingSessionComplete)
		internal annotation class SessionConnectionStatus
	}

	companion object {
		@JvmField
		val buildSessionBroadcast = MagicPropertyBuilder.buildMagicPropertyName(SelectedConnection::class.java, "buildSessionBroadcast")
		@JvmField
		val buildSessionBroadcastStatus = MagicPropertyBuilder.buildMagicPropertyName(SelectedConnection::class.java, "buildSessionBroadcastStatus")
		private val logger = LoggerFactory.getLogger(SelectedConnection::class.java)

		@JvmStatic
		fun getInstance(context: Context): SelectedConnection =
			SelectedConnection(
				MessageBus(LocalBroadcastManager.getInstance(context)),
				SelectedBrowserLibraryIdentifierProvider(context.getApplicationSettingsRepository()),
				ConnectionSessionManager.get(context)
			)
	}
}
