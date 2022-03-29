package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.lazyj.CreateAndHold
import com.namehillsoftware.lazyj.Lazy
import io.mockk.mockk
import org.junit.Test
import org.mockito.Mockito
import org.robolectric.Robolectric

class WhenPlaybackIsPaused : AndroidContext() {
    override fun before() {
        Mockito.`when`(
            notificationContentBuilder.promiseNowPlayingNotification(
                ServiceFile(1),
                true
            )
        )
            .thenReturn(Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())))
        Mockito.`when`(
            notificationContentBuilder.promiseNowPlayingNotification(
                ServiceFile(1),
                false
            )
        )
            .thenReturn(Promise(FakeNotificationCompatBuilder.newFakeBuilder(pausedNotification)))
		val playbackNotificationRouter = PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				NotificationsController(
					service.getObject(),
					notificationManager
				),
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())) },
			mockk(relaxed = true)
		)
        playbackNotificationRouter(PlaybackMessage.PlaybackPaused)
    }

    @Test
    fun thenTheServiceContinuesInTheBackground() {
        Mockito.verify(service.getObject()).stopForeground(false)
    }

    @Test
    fun thenTheNotificationIsNeverSet() {
        Mockito.verify(notificationManager, Mockito.never()).notify(43, pausedNotification)
    }

    companion object {
        private val pausedNotification = Notification()
        private val service: CreateAndHold<Service> = Lazy {
            Mockito.spy(
                Robolectric.buildService(
                    PlaybackService::class.java
                ).get()
            )
        }
        private val notificationManager = Mockito.mock(
            NotificationManager::class.java
        )
        private val notificationContentBuilder = Mockito.mock(
            BuildNowPlayingNotificationContent::class.java
        )
    }
}
