package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatCanFinishPlayback

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
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
		private val volumeManagerUnderTest = NoTransformVolumeManager()

		@JvmStatic
		@BeforeClass
		fun before() {
			val playbackHandler = ResolvablePlaybackHandler()
			val secondPlaybackHandler = ResolvablePlaybackHandler()
			val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
				0,
				playbackHandler,
				NoTransformVolumeManager(),
				ServiceFile(1)))
			val secondPositionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
				0,
				secondPlaybackHandler,
				volumeManagerUnderTest,
				ServiceFile(1)))
			val preparedPlaybackFileQueue = Mockito.mock(PreparedPlayableFileQueue::class.java)
			Mockito.`when`(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(Duration.ZERO))
				.thenReturn(positionedPlaybackHandlerContainer)
				.thenReturn(secondPositionedPlaybackHandlerContainer)
			val playlistPlayback: IPlaylistPlayer = PlaylistPlayer(
				preparedPlaybackFileQueue,
				Duration.ZERO)
			Observable.create(playlistPlayback).subscribe()
			playlistPlayback.setVolume(0.8f)
			playbackHandler.resolve()
		}
	}

	@Test
	fun thenTheVolumeIsChanged() {
		Assertions.assertThat(volumeManagerUnderTest.volume.toFuture().get()).isEqualTo(0.8f)
	}
}
