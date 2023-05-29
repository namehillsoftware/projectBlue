package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused.ThenStartedAgain

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

private const val libraryId = 197
private const val serviceFileId = 97

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

		val nowPlaying = singleNowPlaying(
			LibraryId(libraryId),
			ServiceFile(serviceFileId)
		)

		val messageBus = RecordingApplicationMessageBus()
		PlaybackNotificationBroadcaster(
			mockk {
				every { promiseActiveNowPlaying() } returnsMany listOf(
					singleNowPlaying(LibraryId(libraryId), ServiceFile(1)).toPromise(),
					nowPlaying.toPromise()
				)
			},
			messageBus,
			mockk(),
			notificationController,
			NotificationsConfiguration("", 43),
			notificationContentBuilder,
			mockk {
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns Promise(
					FakeNotificationCompatBuilder.newFakeBuilder(
						ApplicationProvider.getApplicationContext(),
						startingNotification
					)
				)
			}
		)

		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		messageBus.sendMessage(PlaybackMessage.PlaybackPaused)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
	}

	@Test
	fun thenTheLoadingNotificationIsShownManyTimes() {
		verify(exactly = 2) { notificationController.notifyForeground(loadingNotification, 43) }
		verify(exactly = 1) { notificationController.notifyEither(loadingNotification, 43) }
	}

	@Test
	fun thenTheServiceIsStartedOnTheFirstServiceItem() {
		verify(exactly = 1) { notificationController.notifyForeground(startingNotification, 43) }
	}

	@Test
	fun thenTheNotificationIsSetToThePausedNotification() {
		verify(exactly = 1) { notificationController.notifyBackground(firstNotification, 43) }
	}

	@Test
	fun `then the service should stay in the background after changing tracks`() {
		verify(exactly = 1) { notificationController.notifyEither(secondNotification, 43) }
	}

	@Test
	fun thenTheServiceIsStartedOnTheSecondServiceItem() {
		verify { notificationController.notifyForeground(secondNotification, 43) }
	}
}
