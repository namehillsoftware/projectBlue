package com.lasthopesoftware.bluewater.client.playback.service.receivers.notification.GivenAStandardNotificationManager

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.NotifyOfPlaybackEvents
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.PlaybackNotificationRouter
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.service.broadcasters.messages.PlaybackMessage
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenPlaybackIsPaused {

	private val mockNotifier by lazy { mockk<NotifyOfPlaybackEvents>(relaxUnitFun = true) }

	private val mut by lazy {
		val playbackNotificationRouter = PlaybackNotificationRouter(
			mockNotifier,
			mockk(relaxed = true),
			mockk {
				every { promiseUrlKey(any<ServiceFile>()) } answers {
					UrlKeyHolder(
						URL("http://test"),
						firstArg<ServiceFile>()
					).toPromise()
				}
			},
			mockk {
				every { promiseNowPlaying() } returns NowPlaying(
					LibraryId(1),
					listOf(ServiceFile(156)),
					0,
					0L,
					false
				).toPromise()
			},
		)
		playbackNotificationRouter
	}

	@BeforeAll
	fun act() {
		mut(PlaybackMessage.PlaybackPaused)
	}

	@Test
	fun `then notifications are correctly sent`() {
		verify(exactly = 1) { mockNotifier.notifyPaused() }
	}
}
