package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPreparing

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Halting Playback` {

	private val playbackHandler = ResolvablePlaybackHandler()
	private val preparingPlaybackHandler = DeferredPromise(
		PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile("1")
		)
	)

	private var playingFile: PositionedPlayingFile? = null

	@BeforeAll
	fun act() {
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles> {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns preparingPlaybackHandler
		}
		val playlistPlayback = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
		playlistPlayback.resume()
		val futureHalt = playlistPlayback.haltPlayback()
		preparingPlaybackHandler.resolve()
		futureHalt.toExpiringFuture().get()
		playingFile = playlistPlayback.resume().toExpiringFuture().get()
	}

	@Test
	fun `then file preparation is NOT cancelled as the provider should handle cancellation when closed`() {
		assertThat(preparingPlaybackHandler.isCancelled).isFalse()
	}

	@Test
	fun `then playback is never started`() {
		assertThat(playbackHandler.recordedPlayingStates).containsOnly(false)
	}

	@Test
	fun `then the playing file from the second resume is null because the player is no longer active`() {
		assertThat(playingFile).isNull()
	}
}
