package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPreparing.AndPlaybackIsPaused

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
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

class `When Resuming Playback` {

	private val playbackHandler = ResolvablePlaybackHandler()
	private val preparingPlaybackHandler = DeferredPromise(
		PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile("1")
		)
	)

	@BeforeAll
	fun act() {
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles> {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns preparingPlaybackHandler andThen null
		}
		val playlistPlayback = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
		val futureResume = playlistPlayback.resume().toExpiringFuture()
		val futurePause = playlistPlayback.pause().toExpiringFuture()
		preparingPlaybackHandler.resolve()
		futureResume.get()
		futurePause.get()
		playlistPlayback.resume().toExpiringFuture().get()
	}

	@Test
	fun `then playback is only paused and resumed once because it is paused during initial preparation`() {
		assertThat(playbackHandler.recordedPlayingStates).containsExactly(false, true)
	}
}
