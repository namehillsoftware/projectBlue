package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndTheFileHasChanged

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
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

class WhenPlaybackStarts : AndroidContext() {
	companion object {
		private const val libraryId = 99
		private const val serviceFileId = 598
		private val loadingNotification = Notification()
		private val startedNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true)
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		val messageBus = RecordingApplicationMessageBus()

		PlaybackNotificationBroadcaster(
			FakeNowPlayingRepository(singleNowPlaying(LibraryId(libraryId), ServiceFile(serviceFileId))),
			messageBus,
			mockk(),
			notificationController,
			NotificationsConfiguration("", 43),
			mockk {
				every { promiseLoadingNotification(LibraryId(libraryId), any()) } returns newFakeBuilder(context, loadingNotification).toPromise()
				every { promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(serviceFileId), true) } returns newFakeBuilder(context, startedNotification).toPromise()
			},
			mockk {
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns Promise(
					newFakeBuilder(context, Notification())
				)
			},
		)

		messageBus.sendMessage(
			LibraryPlaybackMessage.TrackChanged(
				LibraryId(libraryId), PositionedFile(0, ServiceFile(serviceFileId))
			)
		)
		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
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
