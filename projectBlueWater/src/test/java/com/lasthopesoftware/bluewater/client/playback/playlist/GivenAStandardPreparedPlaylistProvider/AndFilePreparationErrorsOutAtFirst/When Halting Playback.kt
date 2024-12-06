package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.AndFilePreparationErrorsOutAtFirst

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class `When Halting Playback` {

	private val playbackHandler = ResolvablePlaybackHandler()
	private val preparingPlaybackHandler = DeferredPromise(
		PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile(1)
		)
	)

	private val mut by lazy {
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles> {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returnsMany listOf(
				Promise(IOException("It went away!")),
				preparingPlaybackHandler,
				null,
			)
		}

		PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
	}

	private val positionedPlayingFiles = mutableListOf<PositionedPlayingFile>()
	private var resumeException: IOException? = null
	private var haltException: IOException? = null
	private var observationException: IOException? = null

	@BeforeAll
	fun before() {
		val playlistPlayer = mut

		val futurePositionedPlayingFiles = playlistPlayer
			.promisePlayedPlaylist()
			.updates { positionedPlayingFiles.add(it) }
			.toExpiringFuture()

		try {
			playlistPlayer.resume().toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			resumeException = ee.cause as? IOException
		}

		try {
			playlistPlayer.haltPlayback().toExpiringFuture().get()
		} catch (ee: ExecutionException) {
			haltException = ee.cause as? IOException
		}

		try {
			futurePositionedPlayingFiles.get(30, TimeUnit.SECONDS)
		} catch (ee: ExecutionException) {
			observationException = ee.cause as? IOException
		}
	}

	@Test
	fun `then playback does not occur`() {
		assertThat(positionedPlayingFiles).isEmpty()
	}

	@Test
	fun `then the error is correct`() {
		assertThat(resumeException?.message).isEqualTo("It went away!")
	}

	@Test
	fun `then the halting error is the same`() {
		assertThat(haltException).isSameAs(resumeException)
	}

	@Test
	fun `then the observation error is the same`() {
		assertThat(observationException).isSameAs(resumeException)
	}
}
