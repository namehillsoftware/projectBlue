package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager

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
		private val notificationController = mockk<ControlNotifications>()
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		with(notificationContentBuilder) {
			every { getLoadingNotification(any()) } returns newFakeBuilder(context, Notification())
			every { promiseNowPlayingNotification(any(), any()) } returns newFakeBuilder(context, Notification()).toPromise()
		}

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
                        listOf(ServiceFile(100)),
                        0,
                        0L,
                        false,
                    ).toPromise()
                },
            )
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
	}

	@Test
	fun `then the service has not started`() {
		verify(exactly = 0) { notificationController.notifyForeground(any(), any()) }
	}

	@Test
	fun `then the notification has not been broadcast`() {
		verify(exactly = 0) { notificationController.notifyBackground(any(), any()) }
	}
}
