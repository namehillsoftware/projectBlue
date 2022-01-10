package com.lasthopesoftware.bluewater.client.playback.service.notification

import android.app.Notification
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildPlaybackStartingNotification
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications

class PlaybackNotificationBroadcaster(
	private val notificationsController: ControlNotifications,
	notificationsConfiguration: NotificationsConfiguration,
	private val nowPlayingNotificationContentBuilder: BuildNowPlayingNotificationContent,
	private val playbackStartingNotification: BuildPlaybackStartingNotification
) : NotifyOfPlaybackEvents {

	private val notificationId = notificationsConfiguration.notificationId
	private val notificationSync = Any()

	private var isPlaying = false
	private var isNotificationStarted = false
	private var serviceFile: ServiceFile? = null

	override fun notifyPlaying() {
		isPlaying = true

		serviceFile?.also {
			updateNowPlaying(it)
			return
		}

		playbackStartingNotification.promisePreparedPlaybackStartingNotification()
			.then { builder ->
				synchronized(notificationSync) {
					if (isNotificationStarted) return@then
					isNotificationStarted = true
					notificationsController.notifyForeground(builder.build(), notificationId)
				}
			}
	}

	override fun notifyPaused() {
		if (serviceFile == null) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		nowPlayingNotificationContentBuilder
			.promiseNowPlayingNotification(serviceFile, false.also { isPlaying = it })
			.then { builder ->
				notificationsController.notifyBackground(builder.build(), notificationId)
			}
	}

	override fun notifyInterrupted() {
		if (serviceFile == null) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		nowPlayingNotificationContentBuilder
			.promiseNowPlayingNotification(serviceFile, false.also { isPlaying = it })
			.then { builder ->
				notificationsController.notifyForeground(builder.build(), notificationId)
			}
	}

	override fun notifyStopped() {
		synchronized(notificationSync) {
			isPlaying = false
			isNotificationStarted = false
			notificationsController.removeNotification(notificationId)
		}
	}

	override fun notifyPlayingFileChanged(serviceFile: ServiceFile) {
		updateNowPlaying(serviceFile)
	}

	private fun updateNowPlaying(serviceFile: ServiceFile) {
		fun notify(notification: Notification) {
			when {
				isPlaying -> {
					isNotificationStarted = true
					notificationsController.notifyForeground(notification, notificationId)
				}
				isNotificationStarted -> {
					notificationsController.notifyEither(notification, notificationId)
				}
			}
		}

		synchronized(notificationSync) {
			this.serviceFile = serviceFile

			if (!isNotificationStarted && !isPlaying) return

			val loadingBuilderNotification =
				nowPlayingNotificationContentBuilder.getLoadingNotification(isPlaying).build()

			notify(loadingBuilderNotification)

			nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying)
				.then { builder ->
					synchronized(notificationSync) { notify(builder.build()) }
				}
		}
	}
}
