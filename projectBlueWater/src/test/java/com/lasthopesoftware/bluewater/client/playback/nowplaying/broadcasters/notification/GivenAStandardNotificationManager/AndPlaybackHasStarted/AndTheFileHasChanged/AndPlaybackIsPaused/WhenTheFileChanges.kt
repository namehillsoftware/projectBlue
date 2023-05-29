package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused

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

private const val libraryId = 464
private const val serviceFileId = 247

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val firstNotification = Notification()
		private val secondNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		every { notificationContentBuilder.promiseLoadingNotification(LibraryId(libraryId), any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            Notification()
        ).toPromise()
		every { notificationContentBuilder.promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(179), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            firstNotification
        ))
		every { notificationContentBuilder.promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(serviceFileId), false) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            secondNotification
        ))

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
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns Promise(
					FakeNotificationCompatBuilder.newFakeBuilder(
						ApplicationProvider.getApplicationContext(),
						Notification()
					)
				)
			}
		)
		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		messageBus.sendMessage(PlaybackMessage.PlaybackPaused)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
	}

	@Test
	fun `then the service is started in the foreground`() {
		verify(atLeast = 1) { notificationController.notifyForeground(firstNotification, 43) }
	}

	@Test
	fun `then the service should go into the background`() {
		verify(exactly = 1) { notificationController.notifyBackground(firstNotification, 43) }
	}

	@Test
	fun `then the service should stay in the background`() {
		verify(exactly = 1) { notificationController.notifyEither(secondNotification, 43) }
	}
}
