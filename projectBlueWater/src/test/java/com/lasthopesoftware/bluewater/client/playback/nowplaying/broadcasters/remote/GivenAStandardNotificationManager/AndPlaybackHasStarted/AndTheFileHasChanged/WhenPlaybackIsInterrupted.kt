package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

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

class WhenPlaybackIsInterrupted : AndroidContext() {
	companion object {
		private val pausedNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		every { notificationContentBuilder.getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            Notification()
        )
		every { notificationContentBuilder.promiseNowPlayingNotification(any(), any()) } returns Promise(
			FakeNotificationCompatBuilder.newFakeBuilder(
                ApplicationProvider.getApplicationContext(),
                Notification()
            ))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(655), false) } returns Promise(
			FakeNotificationCompatBuilder.newFakeBuilder(
                ApplicationProvider.getApplicationContext(),
                pausedNotification
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
                            Notification()
                        )
                    )
                },
                mockk {
                    every { promiseNowPlaying() } returns NowPlaying(
                        LibraryId(223),
                        listOf(ServiceFile(655)),
                        0,
                        0L,
                        false,
                    ).toPromise()
                },
            )
		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
		playbackNotificationBroadcaster.notifyInterrupted()
	}

	@Test
	fun `then the notification is set to the paused notification`() {
		verify { notificationController.notifyForeground(pausedNotification, 43) }
	}
}
