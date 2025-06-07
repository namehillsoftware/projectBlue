package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class `When Halting Playback` {

	private val affectedSystems by lazy {
		ResolvablePlaybackHandler()
	}
	private val mut by lazy {
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles> {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns PositionedPlayableFile(
				0,
				affectedSystems,
				NoTransformVolumeManager(),
				ServiceFile("1")
			).toPromise()
		}
		PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
	}

	private var isCompleted = false
	private var exception: CancellationException? = null

	@BeforeAll
	fun act() {
		val playlistPlayback = mut
		playlistPlayback.resume()
		val promisedPlayback = playlistPlayback.promisePlayedPlaylist().toExpiringFuture()
		playlistPlayback.haltPlayback().toExpiringFuture().get()
		try {
			promisedPlayback.get(3, TimeUnit.SECONDS)
			isCompleted = false
		} catch (e: ExecutionException) {
			exception = e.cause as? CancellationException
		}
	}

	@Test
	fun `then the playable file is closed`() {
		assertThat(affectedSystems.isClosed).isTrue
	}

	@Test
	fun `then the player feed is not completed`() {
		assertThat(isCompleted).isFalse
	}

	@Test
	fun `then playback resolves with a cancellation exception`() {
		assertThat(exception).isNotNull()
	}
}
