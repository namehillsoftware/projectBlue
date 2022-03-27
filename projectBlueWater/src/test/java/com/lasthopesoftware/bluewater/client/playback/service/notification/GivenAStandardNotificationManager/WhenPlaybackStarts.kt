package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager

import android.app.Notification
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackStart
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenPlaybackStarts : AndroidContext() {

	companion object {
		private val startedNotification = Notification()
		private val notificationController = mockk<ControlNotifications>()
	}

	override fun before() {
		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
		val playbackNotificationRouter = PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				notificationController,
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(startedNotification)) },
			mockk(relaxed = true)
		)
		playbackNotificationRouter(PlaybackStart)
	}

	@Test
	fun thenAStartingNotificationIsSet() {
		verify { notificationController.notifyForeground(startedNotification, 43) }
	}
}
