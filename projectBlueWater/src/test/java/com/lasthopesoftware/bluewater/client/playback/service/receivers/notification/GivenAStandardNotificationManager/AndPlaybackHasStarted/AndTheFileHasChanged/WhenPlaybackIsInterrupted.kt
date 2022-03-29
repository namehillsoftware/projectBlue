package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager.AndPlaybackHasStarted.AndTheFileHasChanged

import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.client.playback.service.notification.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.PlaybackNotificationRouter
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.mockk
import io.mockk.verify
import org.junit.BeforeClass
import org.junit.Test

class WhenPlaybackIsInterrupted {

	companion object {
		private val playbackEventsNotifier by lazy { mockk<NotifyOfPlaybackEvents>(relaxed = true, relaxUnitFun = true) }

		@JvmStatic
		@BeforeClass
		fun before() {
			val recordingApplicationMessageBus = RecordingApplicationMessageBus()
			PlaybackNotificationRouter(playbackEventsNotifier, recordingApplicationMessageBus)

			recordingApplicationMessageBus.sendMessage(PlaybackMessage.PlaybackInterrupted)
		}
	}

	@Test
	fun `then notify of interrupted events`() {
		verify { playbackEventsNotifier.notifyInterrupted() }
	}
}
