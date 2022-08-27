package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AfterPlaybackHasStopped

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.promises.extensions.PromiseMessenger
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val secondNotification = Notification()
		private val service by lazy {
			spyk(Robolectric.buildService(PlaybackService::class.java).get())
		}
		private val notificationManager = mockk<NotificationManager>(relaxUnitFun = true)
	}

    override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		val secondNotificationPromise = PromiseMessenger<NotificationCompat.Builder>()

		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
			every { getLoadingNotification(any()) } returns newFakeBuilder(context, Notification())
			every { promiseNowPlayingNotification(any(), any()) } returns newFakeBuilder(context, Notification()).toPromise()

			every { promiseNowPlayingNotification(ServiceFile(2), any()) } returns secondNotificationPromise
		}

        val playbackNotificationBroadcaster = PlaybackNotificationBroadcaster(
            NotificationsController(
                service,
                notificationManager
            ),
            NotificationsConfiguration("", 43),
            notificationContentBuilder
        ) { Promise(newFakeBuilder(context, Notification())) }

        playbackNotificationBroadcaster.notifyPlaying()
        playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(1))
        playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(2))
        playbackNotificationBroadcaster.notifyStopped()

        secondNotificationPromise.sendResolution(newFakeBuilder(context, secondNotification))
    }

    @Test
    fun `then the service is started in the foreground`() {
        verify(atLeast = 1) { service.startForeground(43, any()) }
    }

    @Test
    fun `then the service does not continue in the background`() {
		verify { service.stopForeground(true) }
    }

    @Test
    fun `then the notification is not set to the second notification`() {
		verify(exactly = 0) { notificationManager.notify(43, secondNotification) }
    }
}
