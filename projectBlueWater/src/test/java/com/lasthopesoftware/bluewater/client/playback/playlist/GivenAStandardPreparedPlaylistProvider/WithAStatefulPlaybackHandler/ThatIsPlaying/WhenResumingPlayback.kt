package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
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
import java.util.concurrent.ExecutionException

class WhenResumingPlayback {

	companion object {
		private var illegalStateException: IllegalStateException? = null
		private var playbackHandler = FakeBufferingPlaybackHandler()
		private var playingFile: PositionedPlayingFile? = null

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
			playlistPlayback.resume().toFuture().get()

			try {
				playingFile = playlistPlayback.resume().toFuture().get()
			} catch (e: ExecutionException) {
				illegalStateException = e.cause as? IllegalStateException
			}
		}
	}

	@Test
	fun thenAnIllegalStateExceptionIsThrownAsItIsIllegalToResumeManyTimes() {
		assertThat(illegalStateException).isNotNull
	}
}
