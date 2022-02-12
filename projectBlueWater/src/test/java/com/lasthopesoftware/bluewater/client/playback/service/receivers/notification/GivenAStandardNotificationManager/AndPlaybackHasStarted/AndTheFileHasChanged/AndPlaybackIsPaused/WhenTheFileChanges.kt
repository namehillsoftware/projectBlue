package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsPaused

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
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
		val playbackNotificationRouter = PlaybackNotificationRouter(PlaybackNotificationBroadcaster(
			NotificationsController(service, notificationManager),
			NotificationsConfiguration("", 43),
			notificationContentBuilder
		) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())) })

		playbackNotificationRouter.onReceive(Intent(PlaylistEvents.onPlaylistStart))

		playbackNotificationRouter.onReceive(
			Intent(PlaylistEvents.onPlaylistTrackChange).putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 1)
		)

		playbackNotificationRouter.onReceive(Intent(PlaylistEvents.onPlaylistPause))

		playbackNotificationRouter.onReceive(
			Intent(PlaylistEvents.onPlaylistTrackChange).putExtra(PlaylistEvents.PlaybackFileParameters.fileKey, 2)
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
