package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification

import android.app.Notification
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildPlaybackStartingNotification
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications

class PlaybackNotificationBroadcaster(
	private val notificationsController: ControlNotifications,
	notificationsConfiguration: NotificationsConfiguration,
	private val nowPlayingNotificationContentBuilder: BuildNowPlayingNotificationContent,
	private val playbackStartingNotification: BuildPlaybackStartingNotification,
) : NotifyOfPlaybackEvents {

	private val notificationId = notificationsConfiguration.notificationId
	private val notificationSync = Any()
	private var isPlaying = false

	private var isNotificationStarted = false
	private var state: Pair<LibraryId, ServiceFile>? = null

	override fun notifyPlaying() {
		isPlaying = true

		state?.also { (libraryId, serviceFile) ->
			updateNowPlaying(libraryId, serviceFile)
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
		val currentState = state
		if (currentState == null) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		val (libraryId, serviceFile) = currentState
		nowPlayingNotificationContentBuilder
			.promiseNowPlayingNotification(libraryId, serviceFile, false.also { isPlaying = false })
			.then {
				it?.apply {
					notificationsController.notifyBackground(build(), notificationId)
				}
			}
	}

	override fun notifyInterrupted() {
		val currentState = state
		if (currentState == null) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		val (libraryId, serviceFile) = currentState
		nowPlayingNotificationContentBuilder
			.promiseNowPlayingNotification(libraryId, serviceFile, false.also { isPlaying = it })
			.then {
				it?.apply {
					notificationsController.notifyForeground(build(), notificationId)
				}
			}
	}

	override fun notifyStopped() {
		synchronized(notificationSync) {
			isPlaying = false
			isNotificationStarted = false
			notificationsController.removeNotification(notificationId)
		}
	}

	override fun notifyPlayingFileUpdated(libraryId: LibraryId, serviceFile: ServiceFile) {
		updateNowPlaying(libraryId, serviceFile)
	}

	private fun updateNowPlaying(libraryId: LibraryId, serviceFile: ServiceFile) {
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
			state = Pair(libraryId, serviceFile)

			fun isValidForNotification() = serviceFile == this.state?.second && (isNotificationStarted || isPlaying)

			nowPlayingNotificationContentBuilder
				.promiseLoadingNotification(libraryId, isPlaying)
				.then { loadingBuilderNotification ->
					synchronized(notificationSync) {
						if (isValidForNotification()) {
							loadingBuilderNotification?.apply { notify(build()) }

							nowPlayingNotificationContentBuilder
								.promiseNowPlayingNotification(libraryId, serviceFile, isPlaying)
								.then {
									it?.apply {
										synchronized(notificationSync) {
											if (isValidForNotification())
												notify(build())
										}
									}
								}
						}
					}
				}
		}
	}
}
