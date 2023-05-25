package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.GivenAStandardNotificationManager

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.PlaybackNotificationBroadcaster
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenPropertiesChangeForADifferentFile {

	private val mockNotifier by lazy { mockk<NotifyOfPlaybackEvents>(relaxUnitFun = true) }

	private val mut by lazy {
		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		PlaybackNotificationBroadcaster(
			recordingApplicationMessageBus,
			mockk {
				every { promiseUrlKey(any(), any<ServiceFile>()) } answers {
					UrlKeyHolder(
						URL("http://test"),
						lastArg<ServiceFile>()
					).toPromise()
				}
			},
			mockNotifier,

		)
		recordingApplicationMessageBus
	}

	@BeforeAll
    fun act() {
		mut.sendMessage(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://test"), ServiceFile(825))))
    }

	@Test
	fun `then notifications are sent correctly`() {
		verify(exactly = 0) { mockNotifier.notifyPlayingFileUpdated(ServiceFile(825)) }
	}
}
