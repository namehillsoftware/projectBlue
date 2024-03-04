package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAnUnplayedFile.AndItHasAnErrorBeforePlaybackStarts

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlaybackException
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenStartingPlayback {
	private var exoPlayerException: ExoPlayerException? = null
	private val eventListener: MutableList<Player.Listener> = ArrayList()

	@BeforeAll
	fun context() {
		val mockExoPlayer = mockk<PromisingExoPlayer>(relaxed = true)
		every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
		every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
		every { mockExoPlayer.getDuration() } returns 100L.toPromise()
		every { mockExoPlayer.addListener(any()) } answers {
			eventListener.add(firstArg())
			mockExoPlayer.toPromise()
		}

		val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)

		eventListener.forEach { e ->
			e.onPlayerError(
				ExoPlaybackException.createForSource(
					IOException(),
					PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
		}

		val futurePlayedFile = exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
			.eventually { obj -> obj.promisePlayedFile() }
			.toExpiringFuture()

		try {
			futurePlayedFile[1, TimeUnit.SECONDS]
		} catch (e: ExecutionException) {
			exoPlayerException = e.cause as? ExoPlayerException
		}
	}

	@Test
	fun `then the playback error is correct`() {
		assertThat(exoPlayerException?.cause).isInstanceOf(ExoPlaybackException::class.java)
	}
}
