package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPausedFile

import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito

class WhenGettingTheFileProgress {

	companion object {
		private var progress: Duration? = null
		private var duration: Duration? = null
		@BeforeClass
		fun before() {
			val mockMediaPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockMediaPlayer.getPlayWhenReady()).thenReturn(false.toPromise())
			Mockito.`when`(mockMediaPlayer.getDuration()).thenReturn(203L.toPromise())
			val exoPlayerFileProgressReader = ExoPlayerPlaybackHandler(mockMediaPlayer)
			progress = exoPlayerFileProgressReader.progress.toFuture().get()
			duration = exoPlayerFileProgressReader.duration.toFuture().get()
		}
	}

	@Test
	fun thenTheFileProgressIsCorrect() {
		AssertionsForClassTypes.assertThat(progress).isEqualTo(Duration.ZERO)
	}

	@Test
	fun thenTheFileDurationIsCorrect() {
		AssertionsForClassTypes.assertThat(duration).isEqualTo(Duration.millis(203))
	}
}
