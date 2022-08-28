package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPausingPlayback {

	private val playbackHandler = FakeBufferingPlaybackHandler()

	@BeforeAll
	fun act() {
		val positionedPlaybackHandlerContainer = Promise(
			PositionedPlayableFile(
				0,
				playbackHandler,
				NoTransformVolumeManager(),
				ServiceFile(1)
			)
		)
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles>().apply {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns positionedPlaybackHandlerContainer
		}
		val playlistPlayback = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
		playlistPlayback.pause()
	}

	@Test
	fun `then playback is paused`() {
		assertThat(playbackHandler.isPlaying).isFalse
	}
}
