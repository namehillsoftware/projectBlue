package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused

import android.app.Notification
import android.app.NotificationManager
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
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
		private val secondNotification = Notification()
		private val service by lazy { spyk(Robolectric.buildService(PlaybackService::class.java).get()) }
		private val notificationManager = mockk<NotificationManager>(relaxed = true, relaxUnitFun = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		every { notificationContentBuilder.getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(Notification())
		every { notificationContentBuilder.promiseNowPlayingNotification(any(), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification()))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(2), false) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(secondNotification))

		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				NotificationsController(service, notificationManager),
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())) },
			recordingApplicationMessageBus
		)

		recordingApplicationMessageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		recordingApplicationMessageBus.sendMessage(
			PlaybackMessage.TrackChanged(
				LibraryId(4),
				PositionedFile(4, ServiceFile(1))
			)
		)

		recordingApplicationMessageBus.sendMessage(PlaybackMessage.PlaybackPaused)

		recordingApplicationMessageBus.sendMessage(
			PlaybackMessage.TrackChanged(
				LibraryId(4),
				PositionedFile(4, ServiceFile(2))
			)
		)
	}

	@Test
	fun thenTheServiceIsStartedInTheForeground() {
		verify(atLeast = 1) { service.startForeground(43, any()) }
	}

	@Test
	fun `then the service goes into the background`() {
		verify(exactly = 1) { service.stopForeground(false) }
	}

	@Test
	fun thenTheNotificationIsSetToThePausedNotification() {
		verify { notificationManager.notify(43, secondNotification) }
	}
}
