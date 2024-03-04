package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenGettingTheFileProgress {

	private var progress: Duration? = null
	private var duration: Duration? = null

	@BeforeAll
	fun act() {
		val mockMediaPlayer = mockk<PromisingExoPlayer>(relaxed = true).apply {
			every { setPlayWhenReady(any()) } returns this.toPromise()
			every { getPlayWhenReady() } returns true.toPromise()
			every { getCurrentPosition() } returns 75L.toPromise()
			every { getDuration() } returns 101L.toPromise()
		}

		val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockMediaPlayer)
		val playback = exoPlayerPlaybackHandler.promisePlayback().toExpiringFuture().get(1, TimeUnit.SECONDS)
		progress = playback?.promisePlayedFile()?.progress?.toExpiringFuture()?.get(1, TimeUnit.SECONDS)
		duration = playback?.duration?.toExpiringFuture()?.get(1, TimeUnit.SECONDS)
	}

	@Test
	fun `then the file progress is correct`() {
		assertThat(progress).isEqualTo(Duration.millis(75))
	}

	@Test
	fun `then the file duration is correct`() {
		assertThat(duration).isEqualTo(Duration.millis(101))
	}
}
