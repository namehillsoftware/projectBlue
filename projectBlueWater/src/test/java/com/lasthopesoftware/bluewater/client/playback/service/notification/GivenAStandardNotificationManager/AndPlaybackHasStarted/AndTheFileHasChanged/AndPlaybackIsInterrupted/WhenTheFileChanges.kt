package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsInterrupted

import android.app.Notification
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val firstNotification = Notification()
		private val secondNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		every { notificationContentBuilder.getLoadingNotification(any()) } returns newFakeBuilder(Notification())
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(1), any()) } returns Promise(newFakeBuilder(firstNotification))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(2), false) } returns Promise(newFakeBuilder(secondNotification))

		val playbackNotificationBroadcaster = PlaybackNotificationBroadcaster(
			notificationController,
			NotificationsConfiguration("", 43),
			notificationContentBuilder
		) { Promise(newFakeBuilder(Notification())) }

		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(1))
		playbackNotificationBroadcaster.notifyInterrupted()
		playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(2))
	}

	@Test
	fun `then the service is always in the foreground`() {
		verify(exactly = 2) { notificationController.notifyForeground(firstNotification, 43) }
	}

	@Test
	fun `then the service stays in the foreground when tracks change`() {
		verify(exactly = 1) { notificationController.notifyEither(secondNotification, 43) }
	}

	@Test
	fun `then the service should never go into the background`() {
		verify(exactly = 0) { notificationController.notifyBackground(any(), any()) }
	}
}