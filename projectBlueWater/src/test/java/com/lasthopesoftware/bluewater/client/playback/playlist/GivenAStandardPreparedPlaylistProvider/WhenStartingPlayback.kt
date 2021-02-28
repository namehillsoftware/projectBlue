package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlayableFileQueue
import com.lasthopesoftware.bluewater.client.playback.file.*
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.reactivex.Observable
import org.assertj.core.api.Assertions
import org.joda.time.Duration
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class WhenStartingPlayback {
	private var positionedPlayingFiles: List<PositionedPlayingFile>? = null
	@Before
	fun before() {
		val mockPlayingFile = Mockito.mock(PlayingFile::class.java)
		Mockito.`when`(mockPlayingFile.promisePlayedFile()).thenReturn(object : ProgressingPromise<Duration, PlayedFile>() {
			override val progress: Promise<Duration>
				get() = Duration.ZERO.toPromise()

			init {
				resolve(Mockito.mock(PlayedFile::class.java))
			}
		})
		val playbackHandler = Mockito.mock(PlayableFile::class.java)
		Mockito.`when`(playbackHandler.promisePlayback()).thenReturn(Promise(mockPlayingFile))
		val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile(1)))
		val preparedPlaybackFileQueue = Mockito.mock(PreparedPlayableFileQueue::class.java)
		Mockito.`when`(preparedPlaybackFileQueue.promiseNextPreparedPlaybackFile(0))
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(positionedPlaybackHandlerContainer)
			.thenReturn(null)
		Observable.create(PlaylistPlayer(preparedPlaybackFileQueue, 0))
			.toList().subscribe { positionedPlayingFiles -> this.positionedPlayingFiles = positionedPlayingFiles }
	}

	@Test
	fun thenThePlaybackCountIsCorrect() {
		Assertions.assertThat(positionedPlayingFiles!!.size).isEqualTo(5)
	}
}
