package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying.AndThenPaused

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito

class WhenPausingAgain {

	companion object {
		private var originalPlayableFile: PositionedPlayableFile? = null
		private var playbackHandler = FakeBufferingPlaybackHandler()
		private var playableFile: PositionedPlayableFile? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			playbackHandler = FakeBufferingPlaybackHandler()
			val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
				0,
				playbackHandler,
				NoTransformVolumeManager(),
				ServiceFile(1)))
			val preparedPlaybackFileQueue = Mockito.mock(PreparedPlayableFileQueue::class.java)
			Mockito.`when`(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(Duration.ZERO))
				.thenReturn(positionedPlaybackHandlerContainer)
			val playlistPlayback = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
			Observable.create(playlistPlayback).subscribe()
			originalPlayableFile = playlistPlayback.pause().toFuture().get()
			playableFile = playlistPlayback.pause().toFuture().get()
		}
	}

	@Test
	fun thenThePlayableFilesAreTheSameBecauseTheFunctionIsIdempotent() {
		assertThat(playableFile).isEqualTo(originalPlayableFile)
	}

	@Test
	fun thenPlaybackIsStillPaused() {
		assertThat(playbackHandler.isPlaying).isFalse
	}

	@Test
	fun thenThePlaylistPositionIsCorrect() {
		assertThat(playableFile?.playlistPosition).isEqualTo(0)
	}

	@Test
	fun thenThePlayableFileIsCorrect() {
		assertThat(playableFile?.serviceFile).isEqualTo(ServiceFile(1))
	}
}
