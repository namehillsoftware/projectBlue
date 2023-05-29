package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

private const val libraryId = 613
private const val serviceFileId = 543

class WhenPlaybackIsInterrupted : AndroidContext() {
	companion object {
		private val pausedNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
		private val notificationContentBuilder by lazy {
			mockk<BuildNowPlayingNotificationContent> {
				every { promiseLoadingNotification(LibraryId(libraryId), any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(
					ApplicationProvider.getApplicationContext(),
					Notification()
				).toPromise()
				every { promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(serviceFileId), any()) } returns Promise(
					FakeNotificationCompatBuilder.newFakeBuilder(
						ApplicationProvider.getApplicationContext(),
						Notification()
					)
				)
				every { promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(serviceFileId), false) } returns Promise(
					FakeNotificationCompatBuilder.newFakeBuilder(
						ApplicationProvider.getApplicationContext(),
						pausedNotification
					)
				)
			}
		}
	}

	override fun before() {
		val nowPlaying = singleNowPlaying(
			LibraryId(libraryId),
			ServiceFile(serviceFileId)
		)

		val messageBus = RecordingApplicationMessageBus()
		PlaybackNotificationBroadcaster(
			mockk {
				every { promiseActiveNowPlaying() } returns nowPlaying.toPromise()
			},
			messageBus,
			mockk(),
			notificationController,
			NotificationsConfiguration(
				"",
				43
			),
			notificationContentBuilder,
			mockk {
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns  Promise(
					FakeNotificationCompatBuilder.newFakeBuilder(
						ApplicationProvider.getApplicationContext(),
						Notification()
					)
				)
			},
		)
		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		messageBus.sendMessage(PlaybackMessage.PlaybackInterrupted)
	}

	@Test
	fun `then the notification is set to the paused notification`() {
		verify { notificationController.notifyForeground(pausedNotification, 43) }
	}
}
