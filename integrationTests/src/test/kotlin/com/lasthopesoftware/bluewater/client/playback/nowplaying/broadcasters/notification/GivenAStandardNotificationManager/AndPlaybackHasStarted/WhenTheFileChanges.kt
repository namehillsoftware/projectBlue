package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.LibraryPlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WhenTheFileChanges : AndroidContext() {
	companion object {
		private const val libraryId = 451
		private const val serviceFileId = "404"

		private val loadingNotification = Notification()
		private val startedNotification = Notification()
		private val notificationController = mockk<ControlNotifications>(relaxUnitFun = true)
	}

	override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()

		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
            every {
                promiseLoadingNotification(
                    LibraryId(libraryId),
                    any()
                )
            } returns com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder(
                context,
                loadingNotification
            ).toPromise()
            every {
                promiseNowPlayingNotification(
                    LibraryId(libraryId),
                    ServiceFile(serviceFileId),
                    true
                )
            } returns com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder(
                context,
                startedNotification
            ).toPromise()
        }

		val messageBus = RecordingApplicationMessageBus()

        PlaybackNotificationBroadcaster(
            mockk {
                every { promiseActiveNowPlaying() } returns com.lasthopesoftware.bluewater.client.playback.nowplaying.singleNowPlaying(
                    LibraryId(libraryId),
                    ServiceFile(serviceFileId)
                ).toPromise()
            },
            messageBus,
            mockk(),
            notificationController,
            NotificationsConfiguration(
                "",
                43
            ),
            notificationContentBuilder,
            mockk {
                every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns Promise(
                    com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder(
                        context,
                        Notification()
                    )
                )
            },
        )
		messageBus.sendMessage(PlaybackMessage.PlaybackStarted)
		messageBus.sendMessage(
            LibraryPlaybackMessage.TrackChanged(
                LibraryId(libraryId), PositionedFile(0, ServiceFile(serviceFileId))
            )
		)
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
