package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused.ThenStartedAgain

import android.app.Notification
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val loadingNotification = Notification()
		private val firstNotification = Notification()
		private val secondNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
	}

	override fun before() {
		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
		every { notificationContentBuilder.getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(loadingNotification)
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(1), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(firstNotification))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(2), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(secondNotification))

		val playbackNotificationRouter = PlaybackNotificationRouter(PlaybackNotificationBroadcaster(
			notificationController,
			NotificationsConfiguration("", 43),
			notificationContentBuilder
		) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(firstNotification)) })

		playbackNotificationRouter.onReceive(ApplicationProvider.getApplicationContext(), Intent(PlaylistEvents.onPlaylistStart))

		run {
			val playlistChangeIntent = Intent(PlaylistEvents.onPlaylistTrackChange)
			playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 1)
			playbackNotificationRouter.onReceive(ApplicationProvider.getApplicationContext(), playlistChangeIntent)
		}

		playbackNotificationRouter.onReceive(ApplicationProvider.getApplicationContext(), Intent(PlaylistEvents.onPlaylistPause))

		run {
			val playlistChangeIntent = Intent(PlaylistEvents.onPlaylistTrackChange)
			playlistChangeIntent.putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 2)
			playbackNotificationRouter
				.onReceive(ApplicationProvider.getApplicationContext(), playlistChangeIntent)
		}

		playbackNotificationRouter.onReceive(ApplicationProvider.getApplicationContext(), Intent(PlaylistEvents.onPlaylistStart))
	}

	@Test
	fun thenTheLoadingNotificationIsCalledCorrectly() {
		verify(exactly = 2) { notificationController.notifyForeground(loadingNotification, 43) }
		verify(exactly = 1) { notificationController.notifyEither(loadingNotification, 43) }
	}

	@Test
	fun thenTheServiceIsStartedOnTheFirstServiceItem() {
		verify(atLeast = 1) { notificationController.notifyForeground(firstNotification, 43) }
	}

	@Test
	fun thenTheNotificationIsSetToThePausedNotification() {
		verify(exactly = 1) { notificationController.notifyEither(secondNotification, 43) }
	}

	@Test
	fun thenTheServiceIsStartedOnTheSecondServiceItem() {
		verify(exactly = 1) { notificationController.notifyForeground(secondNotification, 43) }
	}
}