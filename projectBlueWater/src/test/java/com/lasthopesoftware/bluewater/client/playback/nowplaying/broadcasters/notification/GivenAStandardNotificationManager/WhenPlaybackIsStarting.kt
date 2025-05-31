package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.GivenAStandardNotificationManager

import android.app.Notification
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.notifications.FakeNotificationCompatBuilder
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenPlaybackIsStarting : AndroidContext() {

	companion object {
		private const val libraryId = 728
		private const val serviceFileId = "5ec1495321a14ffcb1c3ec46be7abc39"

		private val startedNotification = Notification()
		private val foregroundNotifications = mutableListOf<Pair<Notification?, Int>>()
	}

	override fun before() {
		val messageBus = RecordingApplicationMessageBus()
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
			mockk {
				every { notifyForeground(any(), any()) } answers {
					foregroundNotifications.add(Pair(firstArg(), secondArg()))
				}
			},
			NotificationsConfiguration(
				"",
				604
			),
			mockk(),
			mockk {
				every { promisePreparedPlaybackStartingNotification(LibraryId(libraryId)) } returns Promise(
					FakeNotificationCompatBuilder.newFakeBuilder(
						ApplicationProvider.getApplicationContext(),
						startedNotification
					)
				)
			},
		)
		messageBus.sendMessage(PlaybackMessage.PlaybackStarting)
	}

	@Test
	fun `then a starting notification is set`() {
		assertThat(foregroundNotifications).containsExactly(Pair(startedNotification, 604))
	}
}
