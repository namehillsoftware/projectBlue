package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.building.BuildNowPlayingNotificationContent
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.android.notifications.control.ControlNotifications
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.AfterClass
import org.junit.Test

class WhenPlaybackIsPaused : AndroidContext() {
	companion object {
		private const val libraryId = 869
		private const val serviceFileId = "585"

		private val pausedNotification = Notification()
		private var controlNotifications: ControlNotifications? = mockk(relaxUnitFun = true)

		@AfterClass
		@JvmStatic
		fun cleanup() {
			controlNotifications = null
		}
	}

    override fun before() {
		val context = ApplicationProvider.getApplicationContext<Context>()
		val notificationContentBuilder = mockk<BuildNowPlayingNotificationContent> {
            every {
                promiseNowPlayingNotification(
                    LibraryId(libraryId),
                    ServiceFile(serviceFileId),
                    true
                )
            } returns Promise(
                com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder(
                    context,
                    Notification()
                )
            )
            every {
                promiseNowPlayingNotification(
                    LibraryId(libraryId),
                    ServiceFile(serviceFileId),
                    false
                )
            } returns Promise(
                com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder.Companion.newFakeBuilder(
                    context,
                    pausedNotification
                )
            )
        }

		val messageBus = com.lasthopesoftware.resources.RecordingApplicationMessageBus()
        PlaybackNotificationBroadcaster(
            mockk {
                every { promiseActiveNowPlaying() } returns NowPlaying(
                    LibraryId(libraryId),
                    listOf(ServiceFile(serviceFileId)),
                    0,
                    0L,
                    false
                ).toPromise()
            },
            messageBus,
            mockk(),
            controlNotifications ?: return,
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
            }
        )

		messageBus.sendMessage(PlaybackMessage.PlaybackPaused)
    }

    @Test
    fun `then the service continues in the background`() {
        verify { controlNotifications?.stopForegroundNotification(43) }
    }

    @Test
    fun `then the notification is never set`() {
        verify(exactly = 0) { controlNotifications?.notifyBackground(pausedNotification, 43) }
    }
}
