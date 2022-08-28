package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
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
		every { notificationContentBuilder.getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            Notification()
        )
		every { notificationContentBuilder.promiseNowPlayingNotification(any(), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            firstNotification
        ))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(2), false) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            secondNotification
        ))

		val playbackNotificationBroadcaster = PlaybackNotificationBroadcaster(
			notificationController,
			NotificationsConfiguration("", 43),
			notificationContentBuilder
		) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(
            ApplicationProvider.getApplicationContext(),
            Notification()
        )) }

		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(1))
		playbackNotificationBroadcaster.notifyPaused()
		playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(2))
	}

	@Test
	fun `then the service is started in the foreground`() {
		verify(atLeast = 1) { notificationController.notifyForeground(firstNotification, 43) }
	}

	@Test
	fun `then the service should go into the background`() {
		verify(exactly = 1) { notificationController.notifyBackground(firstNotification, 43) }
	}

	@Test
	fun `then the service should stay in the background`() {
		verify(exactly = 1) { notificationController.notifyEither(secondNotification, 43) }
	}
}
