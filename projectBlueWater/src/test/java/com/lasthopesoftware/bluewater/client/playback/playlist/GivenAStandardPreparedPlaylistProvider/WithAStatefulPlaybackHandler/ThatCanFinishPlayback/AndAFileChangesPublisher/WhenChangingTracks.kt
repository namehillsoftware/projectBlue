package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback.AndAFileChangesPublisher

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.promises.extensions.onEach
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenChangingTracks {
    private val expectedPositionedPlayableFile = PositionedPlayableFile(
		0,
		FakeBufferingPlaybackHandler(),
		NoTransformVolumeManager(),
		ServiceFile(1)
	)

	private var positionedPlayingFile: PositionedPlayingFile? = null

	@BeforeAll
    fun act() {
        val playbackHandler = ResolvablePlaybackHandler()
        val positionedPlaybackHandlerContainer = Promise(
            PositionedPlayableFile(
                0,
                playbackHandler,
                NoTransformVolumeManager(),
                ServiceFile(1)
            )
        )
        val secondPositionedPlaybackHandlerContainer = Promise(expectedPositionedPlayableFile)
        val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles>().apply {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returnsMany listOf(
				positionedPlaybackHandlerContainer,
				secondPositionedPlaybackHandlerContainer,
			)
		}

		val playlistPlayer = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
		playlistPlayer.resume()
        playlistPlayer.promisePlayedPlaylist().onEach { this.positionedPlayingFile = it }
        playbackHandler.resolve()
    }

    @Test
    fun `then the change is observed`() {
        assertThat(positionedPlayingFile?.asPositionedFile())
            .isEqualTo(expectedPositionedPlayableFile.asPositionedFile())
    }
}
