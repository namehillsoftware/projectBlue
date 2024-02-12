package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler

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

class WhenChangingTheVolume {

	private val volumeManager = NoTransformVolumeManager()

	@BeforeAll
	fun before() {
		val playbackHandler = FakeBufferingPlaybackHandler()
		playbackHandler.promisePlayback()
		val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
			0,
			playbackHandler,
			volumeManager,
			ServiceFile(1)))
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles>().apply {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns positionedPlaybackHandlerContainer
		}
		val playlistPlayback = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
		playlistPlayback.prepare()
		playlistPlayback.setVolume(0.8f)
	}

	@Test
	fun `then the volume is changed`() {
		assertThat(volumeManager.volume.toExpiringFuture().get()).isEqualTo(0.8f)
	}
}
