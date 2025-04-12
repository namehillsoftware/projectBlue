package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged.AfterPlaybackHasStopped

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.FakeNowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import com.lasthopesoftware.bluewater.shared.promises.extensions.PromiseMessenger
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

class WhenTheFileChanges : AndroidContext() {

	companion object {
		private const val libraryId = 434
		private const val serviceFileId = "126"

		private val secondNotification = Notification()
		private val service by lazy {
            spyk(Robolectric.buildService(PlaybackService::class.java).get())
		}
		private val notificationManager = mockk<NotificationManager>(relaxUnitFun = true)
	}

    override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		val secondNotificationPromise = PromiseMessenger<NotificationCompat.Builder?>()

		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
            every {
                promiseLoadingNotification(
                    LibraryId(libraryId),
                    any()
                )
            } returns newFakeBuilder(context, Notification()).toPromise()
            every {
                promiseNowPlayingNotification(
                    LibraryId(libraryId),
                    any(),
                    any()
                )
            } returns newFakeBuilder(context, Notification()).toPromise()
            every {
                promiseNowPlayingNotification(
                    LibraryId(libraryId),
                    ServiceFile(serviceFileId),
                    any()
                )
            } returns secondNotificationPromise
        }

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
            NotificationsController(service, notificationManager),
            NotificationsConfiguration(
                "",
                43
            ),
            notificationContentBuilder,
            mockk {
                every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns Promise(
                    newFakeBuilder(context, Notification())
                )
            },
        )

		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(
            LibraryPlaybackMessage.TrackChanged(
                LibraryId(libraryId),
                nowPlaying.playingFile!!
            )
        )
		nowPlayingRepository.updateNowPlaying(nowPlaying.copy(playlistPosition = 1))
		messageBus.sendMessage(
            LibraryPlaybackMessage.TrackChanged(
                LibraryId(libraryId),
                nowPlaying.playingFile!!
            )
        )
		messageBus.sendMessage(PlaybackMessage.PlaybackStopped)

        secondNotificationPromise.sendResolution(newFakeBuilder(context, secondNotification))
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
