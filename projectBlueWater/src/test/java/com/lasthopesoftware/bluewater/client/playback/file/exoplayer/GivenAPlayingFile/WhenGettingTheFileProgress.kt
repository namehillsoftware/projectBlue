package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenGettingTheFileProgress {

	companion object {
		private var progress: Duration? = null
		private var duration: Duration? = null

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class, TimeoutException::class, ExecutionException::class)
		fun before() {
			val mockMediaPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockMediaPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockMediaPlayer.getCurrentPosition()).thenReturn(75L.toPromise())
			Mockito.`when`(mockMediaPlayer.getDuration()).thenReturn(101L.toPromise())
			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockMediaPlayer)
			val playback = exoPlayerPlaybackHandler.promisePlayback().toFuture().get(1, TimeUnit.SECONDS)
			progress = playback?.promisePlayedFile()?.progress?.toFuture()?.get(1, TimeUnit.SECONDS)
			duration = playback?.duration?.toFuture()?.get(1, TimeUnit.SECONDS)
		}
	}

	@Test
	fun thenTheFileProgressIsCorrect() {
		AssertionsForClassTypes.assertThat(progress).isEqualTo(Duration.millis(75))
	}

	@Test
	fun thenTheFileDurationIsCorrect() {
		AssertionsForClassTypes.assertThat(duration).isEqualTo(Duration.millis(101))
	}
}
