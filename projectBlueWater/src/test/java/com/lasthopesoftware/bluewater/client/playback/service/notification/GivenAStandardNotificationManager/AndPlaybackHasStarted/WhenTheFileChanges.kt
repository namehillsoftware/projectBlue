package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private val loadingNotification = Notification()
		private val startedNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true)
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
			every { getLoadingNotification(any()) } returns newFakeBuilder(context, loadingNotification)
			every { promiseNowPlayingNotification(ServiceFile(1), true) } returns newFakeBuilder(context, startedNotification).toPromise()
		}

		val playbackNotificationBroadcaster = PlaybackNotificationBroadcaster(
			notificationController,
			NotificationsConfiguration("", 43),
			notificationContentBuilder
		) { Promise(newFakeBuilder(context, Notification())) }
		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(1))
	}

	@Test
	fun `then the loading notification is started`() {
		verify { notificationController.notifyForeground(loadingNotification, 43) }
	}

	@Test
	fun `then the service is started in the foreground`() {
		verify { notificationController.notifyForeground(startedNotification, 43) }
	}
}
