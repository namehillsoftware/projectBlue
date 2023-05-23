package com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.GivenAStandardNotificationManager.AndTheFileHasChanged

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
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

class WhenTheFileChanges {

	private val mockNotifier by lazy { mockk<NotifyOfPlaybackEvents>(relaxUnitFun = true) }

	private val mut by lazy {
		val recordingApplicationMessageBus = RecordingApplicationMessageBus()
		val playbackNotificationRouter = PlaybackNotificationRouter(
			mockNotifier,
			recordingApplicationMessageBus,
			mockk {
				every { promiseUrlKey(any(), any<ServiceFile>()) } answers {
					UrlKeyHolder(
						URL("http://test"),
						lastArg<ServiceFile>()
					).toPromise()
				}
			},
		)
		Pair(recordingApplicationMessageBus, playbackNotificationRouter)
	}

	@BeforeAll
    fun act() {
		val (messageBus, _) = mut
		messageBus.sendMessage(PlaybackMessage.TrackChanged(LibraryId(1), PositionedFile(1, ServiceFile(1))))
		messageBus.sendMessage(PlaybackMessage.TrackChanged(LibraryId(1), PositionedFile(1, ServiceFile(1))))
    }

	@Test
	fun `then notification are correctly sent to update the playing file`() {
		verify(exactly = 2) { mockNotifier.notifyPlayingFileUpdated(LibraryId(1), ServiceFile(1)) }
	}
}
