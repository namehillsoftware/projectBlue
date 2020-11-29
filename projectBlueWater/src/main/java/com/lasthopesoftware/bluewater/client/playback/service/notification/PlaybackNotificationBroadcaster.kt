package com.lasthopesoftware.bluewater.client.playback.service.notification

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildPlaybackStartingNotification
import com.lasthopesoftware.resources.notifications.control.ControlNotifications

class PlaybackNotificationBroadcaster(private val notificationsController: ControlNotifications, notificationsConfiguration: NotificationsConfiguration, private val nowPlayingNotificationContentBuilder: BuildNowPlayingNotificationContent, private val playbackStartingNotification: BuildPlaybackStartingNotification) : NotifyOfPlaybackEvents {
	private val notificationId = notificationsConfiguration.notificationId
	private val notificationSync = Any()

	private lateinit var serviceFile: ServiceFile

	private var isPlaying = false
	private var isNotificationStarted = false

	override fun notifyPlaying() {
		isPlaying = true
		if (::serviceFile.isInitialized) {
			updateNowPlaying(serviceFile)
			return
		}

		playbackStartingNotification.promisePreparedPlaybackStartingNotification()
			.then { builder ->
				synchronized(notificationSync) {
					if (!isNotificationStarted) {
						isNotificationStarted = true
						notificationsController.notifyForeground(builder.build(), notificationId)
					}
				}
			}
	}

	override fun notifyPaused() {
		if (!::serviceFile.isInitialized) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		isPlaying = false
		nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, false)
			.then { notificationsController.notifyForeground(it.build(), notificationId) }
	}

	override fun notifyStopped() {
		synchronized(notificationSync) {
			isPlaying = false
			isNotificationStarted = false
			notificationsController.removeNotification(notificationId)
		}
	}

	override fun notifyPlayingFileChanged(serviceFile: ServiceFile) = updateNowPlaying(serviceFile)

	private fun updateNowPlaying(serviceFile: ServiceFile) {
		synchronized(notificationSync) {
			this.serviceFile = serviceFile

			if (!isNotificationStarted && !isPlaying) return

			val loadingBuilderNotification = nowPlayingNotificationContentBuilder.getLoadingNotification(isPlaying).build()

			if (isPlaying) {
				notificationsController.notifyForeground(loadingBuilderNotification, notificationId)
				isNotificationStarted = true
			}

			if (!isPlaying && isNotificationStarted) {
				notificationsController.notifyForeground(loadingBuilderNotification, notificationId)
			}

			nowPlayingNotificationContentBuilder.promiseNowPlayingNotification(serviceFile, isPlaying)
				.then { builder ->
					synchronized(notificationSync) {
						if (!isPlaying) {
							if (!isNotificationStarted) return@then
							notificationsController.notifyForeground(builder.build(), notificationId)
							return@then
						}

						isNotificationStarted = true
						notificationsController.notifyForeground(builder.build(), notificationId)
					}
				}
		}
	}
}
