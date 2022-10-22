package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.AfterClass
import org.junit.Test

class WhenPlaybackIsPaused : AndroidContext() {
	companion object {
		private val pausedNotification = Notification()
		private var controlNotifications: ControlNotifications? = mockk(relaxUnitFun = true)

		@AfterClass
		@JvmStatic
		fun cleanup() {
			controlNotifications = null
		}
	}

    override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
			every { promiseNowPlayingNotification(ServiceFile(1), true) } returns Promise(newFakeBuilder(context, Notification()))
			every { promiseNowPlayingNotification(ServiceFile(1), false) } returns Promise(newFakeBuilder(context, pausedNotification))
		}

		val playbackNotificationBroadcaster = PlaybackNotificationBroadcaster(
			controlNotifications ?: return,
			NotificationsConfiguration(
				"",
				43
			),
			notificationContentBuilder,
			{ Promise(newFakeBuilder(context, Notification())) },
			mockk(),
		)

		playbackNotificationBroadcaster.notifyPaused()
    }

    @Test
    fun `then the service continues in the background`() {
		verify { controlNotifications?.stopForegroundNotification(43) }
    }

    @Test
    fun `then the notification is never set`() {
		verify(exactly = 0) { controlNotifications?.notifyBackground(pausedNotification, 43) }
    }
}
