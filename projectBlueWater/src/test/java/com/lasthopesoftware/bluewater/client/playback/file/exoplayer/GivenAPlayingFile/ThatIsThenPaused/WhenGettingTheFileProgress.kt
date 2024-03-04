package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.ThatIsThenPaused

import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextLong

class WhenGettingTheFileProgress {
	private val progress by lazy {
		val mockMediaPlayer = mockk<PromisingExoPlayer>(relaxed = true).apply {
			every { getPlayWhenReady() } returnsMany listOf(
				true.toPromise(),
				false.toPromise()
			)

			every { getCurrentPosition() } returnsMany listOf(
				78L.toPromise(),
				nextLong().toPromise(),
				nextLong().toPromise(),
			)
		}

		val exoPlayerFileProgressReader = ExoPlayerPlaybackHandler(mockMediaPlayer)
		exoPlayerFileProgressReader.progress
		exoPlayerFileProgressReader.progress.toExpiringFuture().get()
	}

	@Test
	fun `then the file progress is last valid file progress`() {
		assertThat(progress).isEqualTo(Duration.millis(78))
	}
}
