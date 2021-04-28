package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import org.assertj.core.api.Assertions
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito

class WhenChangingTheVolume {

	companion object {
		private val volumeManager = NoTransformVolumeManager()

		@JvmStatic
		@BeforeClass
		fun before() {
			val playbackHandler = FakeBufferingPlaybackHandler()
			playbackHandler.promisePlayback()
			val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
				0,
				playbackHandler,
				volumeManager,
				ServiceFile(1)))
			val preparedPlaybackFileQueue = Mockito.mock(SupplyQueuedPreparedFiles::class.java)
			Mockito.`when`(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(Duration.ZERO))
				.thenReturn(positionedPlaybackHandlerContainer)
			val playlistPlayback: IPlaylistPlayer = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
			Observable.create(playlistPlayback).subscribe()
			playlistPlayback.setVolume(0.8f)
		}
	}

	@Test
	fun thenTheVolumeIsChanged() {
		Assertions.assertThat(volumeManager.volume.toFuture().get()).isEqualTo(0.8f)
	}
}
