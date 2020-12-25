package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.GivenAPlayingFile

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.progress.events.ExoPlayerPlaybackErrorNotifier
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.io.IOException

class WhenAPlaybackErrorOccurs {

	companion object {
		private var exception: ExoPlayerException? = null
		@BeforeClass
		fun before() {
			val exoPlayerPlaybackCompletedNotifier = ExoPlayerPlaybackErrorNotifier(
				ExoPlayerPlaybackHandler(Mockito.mock(ExoPlayer::class.java)))
			exoPlayerPlaybackCompletedNotifier.playbackError { exception = it }
			exoPlayerPlaybackCompletedNotifier.onPlayerError(ExoPlaybackException.createForSource(IOException()))
		}
	}

	@Test
	fun thenThePlaybackErrorIsCorrect() {
		AssertionsForClassTypes.assertThat(exception!!.cause).isInstanceOf(ExoPlaybackException::class.java)
	}
}
