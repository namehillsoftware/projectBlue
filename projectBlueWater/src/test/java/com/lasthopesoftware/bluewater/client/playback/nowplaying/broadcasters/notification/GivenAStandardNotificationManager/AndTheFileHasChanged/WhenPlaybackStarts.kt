package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndTheFileHasChanged

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

class WhenPlaybackStarts : AndroidContext() {
	companion object {
		private val loadingNotification = Notification()
		private val startedNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true)
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
			every { getLoadingNotification(any()) } returns newFakeBuilder(context, loadingNotification)
			every { promiseNowPlayingNotification(ServiceFile(1), true) } returns newFakeBuilder(context, startedNotification).toPromise()
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
                        listOf(ServiceFile(1)),
                        0,
                        0L,
                        false,
                    ).toPromise()
                },
            )
		playbackNotificationBroadcaster.notifyPlayingFileUpdated()
		playbackNotificationBroadcaster.notifyPlaying()
	}

	@Test
	fun `then the loading notification is started`() {
		verify { notificationController.notifyForeground(loadingNotification, 43) }
	}

	@Test
	fun `then the service is started in the foreground`() {
		verify { notificationController.notifyForeground(startedNotification, 43) }
	}
}