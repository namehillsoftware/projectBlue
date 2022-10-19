package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsInterrupted.ThenStartedAgain

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

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
		every { notificationContentBuilder.getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            loadingNotification
        )
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(1), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            firstNotification
        ))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(2), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            secondNotification
        ))

		val playbackNotificationBroadcaster =
            PlaybackNotificationBroadcaster(
                notificationController,
                NotificationsConfiguration(
                    "",
                    43
                ),
                notificationContentBuilder,
                {
                    Promise(
                        FakeNotificationCompatBuilder.newFakeBuilder(
                            ApplicationProvider.getApplicationContext(),
                            startingNotification,
                        ),
                    )
                },
                mockk {
                    every { promiseNowPlaying() } returns NowPlaying(
                        LibraryId(223),
                        listOf(ServiceFile(1)),
                        0,
                        0L,
                        false,
                    ).toPromise() andThen NowPlaying(
                        LibraryId(223),
                        listOf(ServiceFile(2)),
                        0,
                        0L,
                        false,
                    ).toPromise()
                }
            )

		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
		playbackNotificationBroadcaster.notifyInterrupted()
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
		playbackNotificationBroadcaster.notifyPlaying()
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
		verify { notificationController.notifyEither(secondNotification, 43) }
	}

	@Test
	fun thenTheServiceIsStartedOnTheSecondServiceItem() {
		verify { notificationController.notifyForeground(secondNotification, 43) }
	}

	@Test
	fun `then the service should never go into the background`() {
		verify(exactly = 0) { notificationController.notifyBackground(secondNotification, 43) }
	}
}
