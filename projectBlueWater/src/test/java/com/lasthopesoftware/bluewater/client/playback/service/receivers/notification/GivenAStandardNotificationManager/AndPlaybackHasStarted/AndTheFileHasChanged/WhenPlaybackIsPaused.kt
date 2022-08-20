package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class WhenPlaybackIsPaused : AndroidContext() {

	companion object {
		private val pausedNotification = Notification()
		private val service by lazy { spyk(Robolectric.buildService(PlaybackService::class.java).get()) }
		private val notificationManager = mockk<NotificationManager>(relaxed = true, relaxUnitFun = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		every { notificationContentBuilder.getLoadingNotification(any()) } returns newFakeBuilder(context, Notification())
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(1), true) } returns Promise(newFakeBuilder(context, Notification()))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(1), false) } returns Promise(newFakeBuilder(context, pausedNotification))

		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				NotificationsController(service, notificationManager),
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(newFakeBuilder(context, Notification())) },
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
	}

	@Test
	fun thenTheServiceIsInTheForeground() {
		verify { service.startForeground(43, any()) }
	}

	@Test
	fun thenTheServiceNeverGoesToBackground() {
		verify(exactly = 0) { service.stopForeground(43) }
	}

	@Test
	fun thenTheNotificationIsSetToThePausedNotification() {
		verify { notificationManager.notify(43, pausedNotification) }
	}
}
