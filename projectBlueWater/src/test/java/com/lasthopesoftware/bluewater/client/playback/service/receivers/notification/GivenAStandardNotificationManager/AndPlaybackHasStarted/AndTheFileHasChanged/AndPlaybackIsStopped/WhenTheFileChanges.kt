package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsStopped

import android.app.Notification
import android.app.NotificationManager
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistStart
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaylistTrackChange
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.resources.FakeMessageBus
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
		private val service by lazy {
			spyk(Robolectric.buildService(PlaybackService::class.java).get())
		}
		private val notificationManager = mockk<NotificationManager>(relaxUnitFun = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
		private val fakeMessageSender by lazy { FakeMessageBus(ApplicationProvider.getApplicationContext()) }
	}

	override fun before() {
		val builder = mockk<NotificationCompat.Builder>()
		every { builder.build() } returns Notification() andThen secondNotification
		every { notificationContentBuilder.getLoadingNotification(any()) } returns FakeNotificationCompatBuilder.newFakeBuilder(Notification())
		every { notificationContentBuilder.promiseNowPlayingNotification(any(), any()) } returns Promise(builder)
		every { notificationContentBuilder.promiseNowPlayingNotification(ServiceFile(2), any()) } returns Promise(builder)

		val applicationMessageBus = RecordingApplicationMessageBus()
		val playbackNotificationRouter = PlaybackNotificationRouter(
			PlaybackNotificationBroadcaster(
				NotificationsController(service, notificationManager),
				NotificationsConfiguration("", 43),
				notificationContentBuilder
			) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())) },
			applicationMessageBus
		)

		val localPlaybackBroadcaster = LocalPlaybackBroadcaster(fakeMessageSender)
		fakeMessageSender
			.registerReceiver(
				playbackNotificationRouter,
				playbackNotificationRouter.registerForIntents()
					.fold(IntentFilter()) { intentFilter: IntentFilter, action: String? ->
						intentFilter.addAction(action)
						intentFilter
					}
			)

		applicationMessageBus.sendMessage(PlaylistStart)

		applicationMessageBus.sendMessage(PlaylistTrackChange(LibraryId(1), PositionedFile(1, ServiceFile(1))))

		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistStop,
			LibraryId(1),
			PositionedFile(1, ServiceFile(1))
		)

		applicationMessageBus.sendMessage(PlaylistTrackChange(LibraryId(1), PositionedFile(1, ServiceFile(2))))
	}

	@Test
	fun thenTheServiceIsStartedInTheForeground() {
		verify { service.startForeground(43, any()) }
	}

	@Test
	fun thenTheServiceDoesNotContinueInTheBackground() {
		verify { service.stopForeground(true) }
	}

	@Test
	fun thenTheNotificationIsNotSetToTheSecondNotification() {
		verify(exactly = 0) { notificationManager.notify(43, secondNotification) }
	}
}
