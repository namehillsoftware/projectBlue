package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

import android.app.Notification
import android.app.NotificationManager
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackStart
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistTrackChange
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
		private val loadingNotification = Notification()
		private val startedNotification = Notification()
		private val nextNotification = Notification()
		private val service by lazy {
			spyk(Robolectric.buildService(PlaybackService::class.java).get())
		}

		private val notificationManager = mockk<NotificationManager>()
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>().apply {
			every { promiseNowPlayingNotification(any(), any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(startedNotification).toPromise()
			every { promiseNowPlayingNotification(ServiceFile(2), true) } returns FakeNotificationCompatBuilder.newFakeBuilder(nextNotification).toPromise()
			every { getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(loadingNotification)
		}
	}

	override fun before() {
		val playbackNotificationRouter = PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				NotificationsController(service, notificationManager),
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(startedNotification)) },
			mockk(relaxed = true)
		)

		playbackNotificationRouter(PlaybackStart)
		playbackNotificationRouter(PlaylistTrackChange(LibraryId(1), PositionedFile(1, ServiceFile(1))))
	}

    @Test
    fun thenTheServiceIsStartedInTheForeground() {
		verify { service.startForeground(43, startedNotification) }
    }
}
