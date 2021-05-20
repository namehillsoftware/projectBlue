package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsStopped

import android.app.Notification
import android.app.NotificationManager
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import com.annimon.stream.Stream
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.LocalPlaybackBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.PlaylistEvents
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.service.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.service.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.resources.FakeMessageSender
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito
import org.robolectric.Robolectric

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private val secondNotification = Notification()
		private val service = lazy {
			Mockito.spy(Robolectric.buildService(PlaybackService::class.java).get())
		}
		private val notificationManager = Mockito.mock(NotificationManager::class.java)
		private val notificationContentBuilder = Mockito.mock(BuildNowPlayingNotificationContent::class.java)
		private val fakeMessageSender = FakeMessageSender()
	}

	override fun before() {
		val builder = Mockito.mock(NotificationCompat.Builder::class.java)
		Mockito.`when`(builder.build())
			.thenReturn(Notification())
			.thenReturn(secondNotification)
		Mockito.`when`(notificationContentBuilder.getLoadingNotification(anyBoolean()))
			.thenReturn(FakeNotificationCompatBuilder.newFakeBuilder(Notification()))
		Mockito.`when`(notificationContentBuilder.promiseNowPlayingNotification(any(),	anyBoolean()))
			.thenReturn(Promise(builder))
		Mockito.`when`(notificationContentBuilder.promiseNowPlayingNotification(argThat { a -> ServiceFile(2) == a }, anyBoolean()))
			.thenReturn(Promise(builder))
		val playbackNotificationRouter = PlaybackNotificationRouter(PlaybackNotificationBroadcaster(
			NotificationsController(
				service.value,
				notificationManager),
			NotificationsConfiguration("", 43),
			notificationContentBuilder
		) { Promise(FakeNotificationCompatBuilder.newFakeBuilder(Notification())) })

		val localPlaybackBroadcaster = LocalPlaybackBroadcaster(fakeMessageSender)
		fakeMessageSender
			.registerReceiver(
				playbackNotificationRouter,
				Stream.of(playbackNotificationRouter.registerForIntents())
					.reduce(IntentFilter(), { intentFilter: IntentFilter, action: String? ->
						intentFilter.addAction(action)
						intentFilter
					})
			)
		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistStart,
			LibraryId(1),
			PositionedFile(1, ServiceFile(1))
		)
		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistTrackChange,
			LibraryId(1),
			PositionedFile(1, ServiceFile(1))
		)
		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistStop,
			LibraryId(1),
			PositionedFile(1, ServiceFile(1))
		)
		localPlaybackBroadcaster.sendPlaybackBroadcast(
			PlaylistEvents.onPlaylistTrackChange,
			LibraryId(1),
			PositionedFile(1, ServiceFile(2))
		)
	}

	@Test
	fun thenTheServiceIsStartedInTheForeground() {
		Mockito.verify(service.value, Mockito.atLeastOnce()).startForeground(eq(43), any())
	}

	@Test
	fun thenTheServiceDoesNotContinueInTheBackground() {
		Mockito.verify(service.value).stopForeground(true)
	}

	@Test
	fun thenTheNotificationIsNotSetToTheSecondNotification() {
		Mockito.verify(notificationManager, Mockito.never()).notify(43, secondNotification)
	}
}
