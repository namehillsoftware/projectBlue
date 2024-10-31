package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPausedFile

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

class WhenGettingTheFileProgress {

	private var progress: Duration? = null
	private var duration: Duration? = null

	@BeforeAll
	fun act() {
		val mockMediaPlayer = mockk<PromisingExoPlayer>(relaxed = true).apply {
			every { getCurrentPosition() } returns 0L.toPromise()
			every { getPlayWhenReady() } returns false.toPromise()
			every { getDuration() } returns 203L.toPromise()
		}

		val exoPlayerFileProgressReader = ExoPlayerPlaybackHandler(mockMediaPlayer)
		progress = exoPlayerFileProgressReader.progress.toExpiringFuture().get()
		duration = exoPlayerFileProgressReader.duration.toExpiringFuture().get()
	}

	@Test
	fun `then the file progress is correct`() {
		assertThat(progress).isEqualTo(Duration.ZERO)
	}

	@Test
	fun `then the file duration is correct`() {
		assertThat(duration).isEqualTo(Duration.millis(203))
	}
}
