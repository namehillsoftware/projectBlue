package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.GivenAStandardNotificationManager

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenPlaybackIsPaused {

	private val mockNotifier by lazy { mockk<NotifyOfPlaybackEvents>(relaxUnitFun = true) }

	private val mut by lazy {
		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		val playbackNotificationRouter = PlaybackNotificationRouter(
			mockNotifier,
			mockk {
				every { promiseUrlKey(any(), any<ServiceFile>()) } answers {
					UrlKeyHolder(
						URL("http://test"),
						lastArg<ServiceFile>()
					).toPromise()
				}
			},
        )
		recordingApplicationMessageBus
	}

	@BeforeAll
	fun act() {
		mut.sendMessage(PlaybackMessage.PlaybackPaused)
	}

	@Test
	fun `then notifications are correctly sent`() {
		verify(exactly = 1) { mockNotifier.notifyPaused() }
	}
}
