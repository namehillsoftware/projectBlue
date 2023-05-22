package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.GivenAStandardNotificationManager

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenThePropertiesChange {

	private val mockNotifier by lazy { mockk<NotifyOfPlaybackEvents>(relaxUnitFun = true) }

	private val mut by lazy {
		val playbackNotificationRouter = PlaybackNotificationRouter(
			mockNotifier,
            mockk {
				every { promiseUrlKey(any<ServiceFile>()) } answers {
					UrlKeyHolder(
						URL("http://test"),
						firstArg<ServiceFile>()
						).toPromise()
				}
			},
        )
		playbackNotificationRouter
	}

	@BeforeAll
    fun act() {
		mut(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://test"), ServiceFile(635))))
    }

	@Test
	fun `then notification are correctly sent to update the playing file`() {
		verify(exactly = 1) { mockNotifier.notifyPlayingFileUpdated() }
	}
}
