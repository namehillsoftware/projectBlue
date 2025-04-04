package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying.AndThenPaused

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenResumingPlayback {

	private val playbackHandler = FakeBufferingPlaybackHandler()
	private var playingFile: PositionedPlayingFile? = null

	@BeforeAll
	fun act() {
		val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile("1")))
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles>().apply {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns positionedPlaybackHandlerContainer
		}
		val playlistPlayback = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
		playlistPlayback.resume().toExpiringFuture().get()
		playlistPlayback.pause().toExpiringFuture().get()
		playingFile = playlistPlayback.resume().toExpiringFuture().get()
	}

	@Test
	fun `then playback is resumed`() {
		assertThat(playbackHandler.isPlaying).isTrue
	}

	@Test
	fun `then the playlist position is correct`() {
		assertThat(playingFile?.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(playingFile?.serviceFile).isEqualTo(ServiceFile("1"))
	}
}
