package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.net.URL

private const val libraryId = 818

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private val notificationController = mockk<ControlNotifications>()
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		with(notificationContentBuilder) {
			every { promiseLoadingNotification(any(), any()) } returns newFakeBuilder(context, Notification()).toPromise()
			every { promiseNowPlayingNotification(any(), any(), any()) } returns newFakeBuilder(context, Notification()).toPromise()
		}

		val applicationMessages = RecordingApplicationMessageBus()

		PlaybackNotificationBroadcaster(
			applicationMessages,
			mockk {
				every { promiseUrlKey(LibraryId(libraryId), ServiceFile(860)) } returns UrlKeyHolder(
					URL("http://test"),
					ServiceFile(860)
				).toPromise()
			},
			notificationController,
			NotificationsConfiguration(
				"",
				43
			),
			notificationContentBuilder,
			mockk {
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns Promise(newFakeBuilder(context, Notification()))
			}
		)

		applicationMessages.sendMessage(
			LibraryPlaybackMessage.TrackChanged(LibraryId(1), PositionedFile(1, ServiceFile(1))))
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
