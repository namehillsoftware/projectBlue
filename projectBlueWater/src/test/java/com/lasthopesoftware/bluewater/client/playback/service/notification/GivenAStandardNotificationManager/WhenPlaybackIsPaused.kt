package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.AfterClass
import org.junit.Test
import org.robolectric.Robolectric

class WhenPlaybackIsPaused : AndroidContext() {
	companion object {
		private val pausedNotification = Notification()
		private var service: Lazy<PlaybackService>? = lazy {
			spyk(Robolectric.buildService(PlaybackService::class.java).get())
		}
		private val notificationManager = mockk<NotificationManager>()

		@AfterClass
		@JvmStatic
		fun cleanup() {
			service = null
		}
	}

    override fun before() {
		val service = service?.value ?: return

		val context = ApplicationProvider.getApplicationContext<Context>()
		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
			every { promiseNowPlayingNotification(ServiceFile(1), true) } returns Promise(newFakeBuilder(context, Notification()))
			every { promiseNowPlayingNotification(ServiceFile(1), false) } returns Promise(newFakeBuilder(context, pausedNotification))
		}

		val playbackNotificationRouter = PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				NotificationsController(
					service,
					notificationManager
				),
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(newFakeBuilder(context, Notification())) },
			mockk(relaxed = true),
		)
        playbackNotificationRouter(PlaybackMessage.PlaybackPaused)
    }

    @Test
    fun `then the service continues in the background`() {
		verify { service?.value?.stopForeground(false) }
    }

    @Test
    fun `then the notification is never set`() {
		verify(exactly = 0) { notificationManager.notify(43, pausedNotification) }
    }
}
