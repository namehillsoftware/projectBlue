package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification

import androidx.core.app.NotificationCompat
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.android.notifications.ProduceNotificationBuilders
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.promises.extensions.ContinuableResult
import com.lasthopesoftware.promises.extensions.ContinuingResult
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.ProgressingPromiseProxy
import com.lasthopesoftware.resources.strings.GetStringResources
import com.namehillsoftware.handoff.promises.response.ImmediateAction
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class NotifyingLibraryConnectionProvider(
	private val notificationBuilders: ProduceNotificationBuilders,
	private val inner: ProvideLibraryConnections,
	private val notificationsConfiguration: NotificationsConfiguration,
	private val notifications: ControlNotifications,
	private val stringResources: GetStringResources,
) : ProvideLibraryConnections {
	override fun promiseLibraryConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, ProvideConnections?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, ProvideConnections?>(inner.promiseLibraryConnection(libraryId)),
			ImmediateResponse<ContinuableResult<BuildingConnectionStatus>, Unit>,
			ImmediateAction {

			private val sync = Any()

			@Volatile
			private var isFinished = false

			init {
				must(this)
				progress.then(this)
			}

			override fun respond(continuable: ContinuableResult<BuildingConnectionStatus>) {
				if (isFinished) return

				synchronized(sync) {
					if (isFinished) return

					val notifyBuilder =
						notificationBuilders.getNotificationBuilder(notificationsConfiguration.notificationChannel)
					notifyBuilder
						.setOngoing(false)
						.setContentTitle(stringResources.connectingToServerTitle)

					if (continuable !is ContinuingResult) return

					when (continuable.current) {
						BuildingConnectionStatus.GettingLibrary -> notifyBuilder.setContentText(stringResources.gettingLibrary)
						BuildingConnectionStatus.SendingWakeSignal -> notifyBuilder.setContentText(stringResources.sendingWakeSignal)
						BuildingConnectionStatus.BuildingConnection -> notifyBuilder.setContentText(stringResources.connectingToServerLibrary)
						BuildingConnectionStatus.BuildingConnectionComplete -> notifyBuilder.setContentText(
							stringResources.connected
						)

						BuildingConnectionStatus.GettingLibraryFailed, BuildingConnectionStatus.BuildingConnectionFailed -> return
					}

					notifications.notifyForeground(
						buildFullNotification(notifyBuilder),
						notificationsConfiguration.notificationId
					)

					continuable.next.then(this)
				}
			}

			override fun act() = synchronized(sync) {
				isFinished = true
				notifications.removeNotification(notificationsConfiguration.notificationId)
			}
		}

	private fun buildFullNotification(notificationBuilder: NotificationCompat.Builder) =
		notificationBuilder
			.setSmallIcon(R.drawable.now_playing_status_icon_white)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.build()
}
