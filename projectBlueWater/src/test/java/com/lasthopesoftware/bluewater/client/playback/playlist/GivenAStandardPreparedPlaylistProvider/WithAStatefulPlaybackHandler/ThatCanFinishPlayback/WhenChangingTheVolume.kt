package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenChangingTheVolume {

	private val volumeManagerUnderTest = NoTransformVolumeManager()

	@BeforeAll
	fun before() {
		val playbackHandler = ResolvablePlaybackHandler()
		val secondPlaybackHandler = ResolvablePlaybackHandler()
		val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile("1")))
		val secondPositionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
			0,
			secondPlaybackHandler,
			volumeManagerUnderTest,
			ServiceFile("1")))
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles>().apply {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returnsMany listOf(
				positionedPlaybackHandlerContainer,
				secondPositionedPlaybackHandlerContainer,
			)
		}

		val playlistPlayback = PlaylistPlayer(
			preparedPlaybackFileQueue,
			Duration.ZERO)
		playlistPlayback.resume()
		playlistPlayback.setVolume(0.8f)
		playbackHandler.resolve()
	}

	@Test
	fun thenTheVolumeIsChanged() {
		assertThat(volumeManagerUnderTest.volume.toExpiringFuture().get()).isEqualTo(0.8f)
	}
}
