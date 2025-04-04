package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsInterrupted

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private const val libraryId = 769
		private const val serviceFileId = "802"

		private val firstNotification = Notification()
		private val secondNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true, relaxed = true)
		private val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent>()
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		every { notificationContentBuilder.promiseLoadingNotification(LibraryId(libraryId), any()) } returns newFakeBuilder(context, Notification()).toPromise()
		every { notificationContentBuilder.promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile("1"), any()) } returns Promise(newFakeBuilder(context, firstNotification))
		every { notificationContentBuilder.promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(serviceFileId), false) } returns Promise(newFakeBuilder(context, secondNotification))

		val nowPlaying = NowPlaying(
			LibraryId(libraryId),
			listOf(ServiceFile("1"), ServiceFile(serviceFileId)),
			0,
			0,
			false,
		)

		val nowPlayingRepository = FakeNowPlayingRepository(nowPlaying)

		val messageBus = RecordingApplicationMessageBus()
		PlaybackNotificationBroadcaster(
			nowPlayingRepository,
			messageBus,
			mockk(),
			notificationController,
			NotificationsConfiguration(
				"",
				43
			),
			notificationContentBuilder,
			mockk {
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns	Promise(
					newFakeBuilder(context, Notification())
				)
		  	},
		)

		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		nowPlayingRepository.updateNowPlaying(nowPlaying.copy(playlistPosition = 1))
		messageBus.sendMessage(PlaybackMessage.PlaybackInterrupted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
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
