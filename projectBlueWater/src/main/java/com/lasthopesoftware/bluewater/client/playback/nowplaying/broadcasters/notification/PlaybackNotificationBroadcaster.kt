package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification

import android.app.Notification
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildPlaybackStartingNotification
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications

class PlaybackNotificationBroadcaster(
	private val notificationsController: ControlNotifications,
	notificationsConfiguration: NotificationsConfiguration,
	private val nowPlayingNotificationContentBuilder: BuildNowPlayingNotificationContent,
	private val playbackStartingNotification: BuildPlaybackStartingNotification,
	private val nowPlayingProvider: GetNowPlayingState,
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
		val serviceFile = serviceFile
		if (serviceFile == null) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		nowPlayingNotificationContentBuilder
			.promiseNowPlayingNotification(serviceFile, false.also { isPlaying = false })
			.then { builder ->
				notificationsController.notifyBackground(builder.build(), notificationId)
			}
	}

	override fun notifyInterrupted() {
		val serviceFile = serviceFile
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

	override fun notifyPlayingFileUpdated() {
		nowPlayingProvider.promiseNowPlaying().then { it?.playingFile?.serviceFile?.also(::updateNowPlaying) }
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

			nowPlayingNotificationContentBuilder
				.promiseNowPlayingNotification(serviceFile, isPlaying)
				.then { builder ->
					synchronized(notificationSync) { notify(builder.build()) }
				}
		}
	}
}