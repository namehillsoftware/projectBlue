package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.ThatIsThenPaused

import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class WhenGettingTheFileProgress {
	@Test
	fun thenTheFileProgressIsLastValidFileProgress() {
		AssertionsForClassTypes.assertThat(progress).isEqualTo(Duration.millis(78))
	}

	companion object {
		private var progress: Duration? = null
		@BeforeClass
		fun before() {
			val mockMediaPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockMediaPlayer.getPlayWhenReady())
				.thenReturn(true.toPromise())
				.thenReturn(false.toPromise())
			Mockito.`when`(mockMediaPlayer.getCurrentPosition())
				.thenReturn(78L.toPromise())
				.thenReturn(Random().nextLong().toPromise())
				.thenReturn(Random().nextLong().toPromise())
			val exoPlayerFileProgressReader = ExoPlayerPlaybackHandler(mockMediaPlayer)
			exoPlayerFileProgressReader.progress
			progress = exoPlayerFileProgressReader.progress.toFuture().get()
		}
	}
}
