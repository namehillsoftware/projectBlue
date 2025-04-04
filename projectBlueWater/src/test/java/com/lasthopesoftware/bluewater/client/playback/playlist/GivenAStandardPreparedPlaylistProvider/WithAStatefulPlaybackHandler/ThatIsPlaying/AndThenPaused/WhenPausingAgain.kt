package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying.AndThenPaused

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
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

class WhenPausingAgain {

	private val playbackHandler = FakeBufferingPlaybackHandler()
	private var originalPlayableFile: PositionedPlayableFile? = null
	private var playableFile: PositionedPlayableFile? = null

	@BeforeAll
	fun before() {
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
		originalPlayableFile = playlistPlayback.pause().toExpiringFuture().get()
		playableFile = playlistPlayback.pause().toExpiringFuture().get()
	}

	@Test
	fun `then the playable files are the same because the function is idempotent`() {
		assertThat(playableFile).isEqualTo(originalPlayableFile)
	}

	@Test
	fun `then playback is still paused`() {
		assertThat(playbackHandler.isPlaying).isFalse
	}

	@Test
	fun `then the playlist position is correct`() {
		assertThat(playableFile?.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then the playable file is correct`() {
		assertThat(playableFile?.serviceFile).isEqualTo(ServiceFile("1"))
	}
}
