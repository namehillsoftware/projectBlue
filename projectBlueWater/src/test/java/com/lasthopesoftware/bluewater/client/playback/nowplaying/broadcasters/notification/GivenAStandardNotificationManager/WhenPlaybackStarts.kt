package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
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
		val playbackNotificationBroadcaster =
            PlaybackNotificationBroadcaster(
                notificationController,
                NotificationsConfiguration(
                    "",
                    43
                ),
                notificationContentBuilder,
                {
                    Promise(
                        FakeNotificationCompatBuilder.newFakeBuilder(
                            ApplicationProvider.getApplicationContext(),
                            startedNotification
                        )
                    )
                },
            )
		playbackNotificationBroadcaster.notifyPlaying()
	}

	@Test
	fun thenAStartingNotificationIsSet() {
		verify { notificationController.notifyForeground(startedNotification, 43) }
	}
}
