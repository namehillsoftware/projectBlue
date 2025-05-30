package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification

import android.app.Notification
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideUrlKey
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildPlaybackStartingNotification
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver

class PlaybackNotificationBroadcaster(
	private val nowPlayingState: GetNowPlayingState,
	registerApplicationMessages: RegisterForApplicationMessages,
	private val urlKeys: ProvideUrlKey,
	private val notificationsController: ControlNotifications,
	notificationsConfiguration: NotificationsConfiguration,
	private val nowPlayingNotificationContentBuilder: BuildNowPlayingNotificationContent,
	private val playbackStartingNotification: BuildPlaybackStartingNotification,
) : PlaybackNotificationRouter(registerApplicationMessages), AutoCloseable {

	private val notificationId = notificationsConfiguration.notificationId
	private val notificationSync = Any()
	private var isPlaying = false

	private var isNotificationStarted = false
	private var libraryId: LibraryId? = null
	private var serviceFile: ServiceFile? = null

	private val filePropertiesUpdateSubscription = registerApplicationMessages.registerReceiver { m: FilePropertiesUpdatedMessage ->
		libraryId?.also { l ->
			serviceFile?.also { s ->
				urlKeys
					.promiseUrlKey(l, s)
					.then { currentUrlKeyHolder ->
						if (m.urlServiceKey == currentUrlKeyHolder)
							updateNowPlaying(l, s)
					}
			}
		}
	}

	override fun close() {
		filePropertiesUpdateSubscription.close()
		super.close()
	}

	override fun notifyStarting() {
		nowPlayingState
			.promiseActiveNowPlaying()
			.then { np ->
				np?.libraryId?.also { libraryId ->
					this.libraryId = libraryId
					playbackStartingNotification
						.promisePreparedPlaybackStartingNotification(libraryId)
						.then { builder ->
							synchronized(notificationSync) {
								if (!isNotificationStarted) {
									isNotificationStarted = true
									notificationsController.notifyForeground(builder.build(), notificationId)
								}
							}
						}
				}
			}
	}

	override fun notifyPlaying() {
		isPlaying = true

		libraryId?.also { libraryId ->
			serviceFile?.also { serviceFile ->
				updateNowPlaying(libraryId, serviceFile)
				return
			}
		}

		notifyStarting()
	}

	override fun notifyPaused() {
		val currentServiceFile = serviceFile
		val libraryId = libraryId
		if (currentServiceFile == null || libraryId == null) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		nowPlayingNotificationContentBuilder
			.promiseNowPlayingNotification(libraryId, currentServiceFile, false)
			.then { it ->
				it?.apply {
					notificationsController.notifyBackground(build(), notificationId)
				}
			}

		isPlaying = false
	}

	override fun notifyInterrupted() {
		val currentServiceFile = serviceFile
		val libraryId = libraryId
		if (currentServiceFile == null || libraryId == null) {
			notificationsController.stopForegroundNotification(notificationId)
			return
		}

		nowPlayingNotificationContentBuilder
			.promiseNowPlayingNotification(libraryId, currentServiceFile, false)
			.then { it ->
				it?.apply {
					notificationsController.notifyForeground(build(), notificationId)
				}
			}

		isPlaying = false
	}

	override fun notifyStopped() = notifyPaused()

	override fun notifyPlayingFileUpdated() {
		nowPlayingState.promiseActiveNowPlaying().then { np ->
			np?.playingFile?.serviceFile?.also { sf -> updateNowPlaying(np.libraryId, sf) }
		}
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
			this.libraryId = libraryId
			this.serviceFile = serviceFile

			fun isValidForNotification() = serviceFile == this.serviceFile && libraryId == this.libraryId && (isNotificationStarted || isPlaying)

			nowPlayingNotificationContentBuilder
				.promiseLoadingNotification(libraryId, isPlaying)
				.then { loadingBuilderNotification ->
					synchronized(notificationSync) {
						if (isValidForNotification()) {
							loadingBuilderNotification?.apply { notify(build()) }

							nowPlayingNotificationContentBuilder
								.promiseNowPlayingNotification(libraryId, serviceFile, isPlaying)
								.then { it ->
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
