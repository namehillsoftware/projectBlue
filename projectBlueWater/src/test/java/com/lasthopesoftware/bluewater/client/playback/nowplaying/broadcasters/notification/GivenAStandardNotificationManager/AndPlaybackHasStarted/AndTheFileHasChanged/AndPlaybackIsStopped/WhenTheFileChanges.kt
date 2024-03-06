package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AndPlaybackIsStopped

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

private const val libraryId = 510
private const val serviceFileId = 249

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private val secondNotification = Notification()
		private val service by lazy {
			spyk(Robolectric.buildService(PlaybackService::class.java).get())
		}
		private val notificationManager = mockk<NotificationManager>(relaxUnitFun = true)
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
			every { promiseLoadingNotification(LibraryId(libraryId), any()) } returns newFakeBuilder(context, Notification()).toPromise()
			every { promiseNowPlayingNotification(LibraryId(libraryId), any(), any()) } returns newFakeBuilder(
				context,
				Notification()
			).toPromise()
			every { promiseNowPlayingNotification(LibraryId(libraryId), ServiceFile(serviceFileId), any()) } returns newFakeBuilder(
				context,
				secondNotification
			).toPromise()
		}

		val nowPlaying = singleNowPlaying(
			LibraryId(libraryId),
			ServiceFile(serviceFileId)
		)

		val messageBus = RecordingApplicationMessageBus()
		PlaybackNotificationBroadcaster(
			mockk {
				every { promiseActiveNowPlaying() } returns nowPlaying.toPromise()
			},
			messageBus,
			mockk(),
			NotificationsController(service, notificationManager),
			NotificationsConfiguration("", 43),
			notificationContentBuilder,
			mockk {
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns  Promise(
					newFakeBuilder(
						ApplicationProvider.getApplicationContext(),
						Notification()
					)
				)
			},
		)
		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
		messageBus.sendMessage(PlaybackMessage.PlaybackStopped)
		messageBus.sendMessage(LibraryPlaybackMessage.TrackChanged(LibraryId(libraryId), nowPlaying.playingFile!!))
	}

	@Test
	fun `then the service is started in the foreground`() {
		verify(atLeast = 1) { service.startForeground(43, any()) }
	}

	@Test
	fun `then the service does not continue in the background`() {
		verify { service.stopForeground(Service.STOP_FOREGROUND_DETACH) }
	}

	@Test
	fun `then the notification is set to the second notification`() {
		verify { notificationManager.notify(43, secondNotification) }
	}
}
