package com.lasthopesoftware.bluewater.client.playback.service.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

import android.app.Notification
import android.app.NotificationManager
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
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
		every { notificationContentBuilder.getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(Notification())
		every { notificationContentBuilder.promiseNowPlayingNotification(any(), any()) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification()))
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(1), false) } returns Promise(FakeNotificationCompatBuilder.newFakeBuilder(pausedNotification))
		val playbackNotificationBroadcaster = PlaybackNotificationBroadcaster(
			NotificationsController(service, notificationManager),
			NotificationsConfiguration("", 43),
			notificationContentBuilder
		) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())) }
		playbackNotificationBroadcaster.notifyPlaying()
		playbackNotificationBroadcaster.notifyPlayingFileChanged(ServiceFile(1))
		playbackNotificationBroadcaster.notifyPaused()
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