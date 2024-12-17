package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressedPromise
import com.lasthopesoftware.promises.extensions.onEach
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenStartingPlayback {
	private val positionedPlayingFiles = mutableListOf<PositionedPlayingFile>()

	@BeforeAll
	fun before() {
		val mockPlayingFile = mockk<PlayingFile> {
			every { promisePlayedFile() } returns object : ProgressedPromise<Duration, PlayedFile>() {
				override val progress: Promise<Duration>
					get() = Duration.ZERO.toPromise()

				init {
					resolve(mockk())
				}
			}
		}

		val playbackHandler = mockk<PlayableFile>(relaxUnitFun = true) {
			every { promisePlayback() } returns Promise(mockPlayingFile)
		}

		val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile(1)))

		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles> {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returnsMany listOf(
				positionedPlaybackHandlerContainer,
				positionedPlaybackHandlerContainer,
				positionedPlaybackHandlerContainer,
				positionedPlaybackHandlerContainer,
				positionedPlaybackHandlerContainer,
				null,
			)
		}

		val playlistPlayer = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)

		val futurePositionedPlayingFiles = playlistPlayer
			.promisePlayedPlaylist()
			.onEach(positionedPlayingFiles::add)
			.toExpiringFuture()

		playlistPlayer.resume()

		futurePositionedPlayingFiles.get(30, TimeUnit.SECONDS)
	}

	@Test
	fun `then the playback count is correct`() {
		assertThat(positionedPlayingFiles.size).isEqualTo(5)
	}
}
