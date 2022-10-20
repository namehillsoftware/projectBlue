package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.remote.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsInterrupted

import android.app.Notification
import android.content.Context
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
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val firstNotification = Notification()
		private val secondNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		every { notificationContentBuilder.getLoadingNotification(any()) } returns newFakeBuilder(context, Notification())
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(1), any()) } returns Promise(newFakeBuilder(context, firstNotification))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(2), false) } returns Promise(newFakeBuilder(context, secondNotification))

		val playbackNotificationBroadcaster =
            PlaybackNotificationBroadcaster(
                notificationController,
                NotificationsConfiguration(
                    "",
                    43
                ),
                notificationContentBuilder,
                { Promise(newFakeBuilder(context, Notification())) },
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
                },
            )

		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
		playbackNotificationBroadcaster.notifyInterrupted()
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
	}

	@Test
	fun `then the service is always in the foreground`() {
		verify(exactly = 2) { notificationController.notifyForeground(firstNotification, 43) }
	}

	@Test
	fun `then the service stays in the foreground when tracks change`() {
		verify(exactly = 1) { notificationController.notifyEither(secondNotification, 43) }
	}

	@Test
	fun `then the service should never go into the background`() {
		verify(exactly = 0) { notificationController.notifyBackground(any(), any()) }
	}
}
