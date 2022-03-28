package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager

import android.app.Notification
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistMessages
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val service by lazy {
			spyk(Robolectric.buildService(PlaybackService::class.java).get())
		}

		private val notificationManager = mockk<NotificationManager>()
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>().apply {
			every { promiseNowPlayingNotification(any(), any()) } returns mockk<NotificationCompat.Builder>().toPromise()
		}
	}

    override fun before() {
		val playbackNotificationRouter = PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				NotificationsController(
					service,
					notificationManager
				),
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())) },
			mockk(relaxed = true)
		)

		playbackNotificationRouter(PlaylistMessages.TrackChanged(LibraryId(1), PositionedFile(1, ServiceFile(1))))
    }

    @Test
    fun thenTheServiceHasNotStarted() {
        verify(exactly = 0) { service.startForeground(any(), any()) }
    }

    @Test
    fun thenTheNotificationHasNotBeenBroadcast() {
		verify(exactly = 0) { notificationManager.notify(any(), any()) }
    }
}
