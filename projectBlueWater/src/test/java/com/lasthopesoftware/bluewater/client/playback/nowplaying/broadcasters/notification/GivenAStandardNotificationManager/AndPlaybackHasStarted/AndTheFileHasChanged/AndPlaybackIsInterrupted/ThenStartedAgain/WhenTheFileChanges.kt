package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsInterrupted.ThenStartedAgain

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

private const val libraryId = 5
private const val serviceFileId = 615

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val loadingNotification = Notification()
		private val startingNotification = Notification()
		private val firstNotification = Notification()
		private val secondNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		every { notificationContentBuilder.promiseLoadingNotification(LibraryId(libraryId), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            loadingNotification
        ))
		every { notificationContentBuilder.promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(1), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            firstNotification
        ))
		every { notificationContentBuilder.promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(serviceFileId), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            secondNotification
        ))

		val nowPlaying = NowPlaying(
			LibraryId(libraryId),
			listOf(ServiceFile(1), ServiceFile(serviceFileId)),
			0,
			0,
			false,
		)

		val nowPlayingRepository = FakeNowPlayingRepository(nowPlaying)

		val messageBus = RecordingApplicationMessageBus()
		PlaybackNotificationBroadcaster(
			nowPlayingRepository,
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
						startingNotification,
					),
				)
			}
		)

		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		nowPlayingRepository.updateNowPlaying(nowPlaying.copy(playlistPosition = 1))
		messageBus.sendMessage(PlaybackMessage.PlaybackInterrupted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
	}

	@Test
	fun `then the loading notification is shown many times`() {
		verify(exactly = 2) { notificationController.notifyForeground(loadingNotification, 43) }
		verify(exactly = 1) { notificationController.notifyEither(loadingNotification, 43) }
	}

	@Test
	fun `then the service is started on the first service item`() {
		verify(exactly = 1) { notificationController.notifyForeground(startingNotification, 43) }
	}

	@Test
	fun `then the notification is set to the paused notification`() {
		verify { notificationController.notifyEither(secondNotification, 43) }
	}

	@Test
	fun `then the service is started on the second service item`() {
		verify { notificationController.notifyForeground(secondNotification, 43) }
	}

	@Test
	fun `then the service should never go into the background`() {
		verify(exactly = 0) { notificationController.notifyBackground(secondNotification, 43) }
	}
}
