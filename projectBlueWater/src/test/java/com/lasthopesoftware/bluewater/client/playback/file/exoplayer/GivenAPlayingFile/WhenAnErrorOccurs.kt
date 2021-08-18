package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenAnErrorOccurs {

	companion object {
		private var exoPlayerException: ExoPlayerException? = null
		private val eventListener: MutableList<Player.Listener> = ArrayList()

		@JvmStatic
		@BeforeClass
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
			val futurePlayedFile = exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { obj -> obj.promisePlayedFile() }
				.toFuture()
			eventListener.forEach { e ->
				e.onPlayerError(ExoPlaybackException.createForSource(
					IOException(),
					PlaybackException.ERROR_CODE_IO_UNSPECIFIED))
			}

			try {
				futurePlayedFile[1, TimeUnit.SECONDS]
			} catch (e: ExecutionException) {
				exoPlayerException = e.cause as? ExoPlayerException
			}
		}
	}

	@Test
	fun thenThePlaybackErrorIsCorrect() {
		assertThat(exoPlayerException?.cause).isInstanceOf(ExoPlaybackException::class.java)
	}
}
