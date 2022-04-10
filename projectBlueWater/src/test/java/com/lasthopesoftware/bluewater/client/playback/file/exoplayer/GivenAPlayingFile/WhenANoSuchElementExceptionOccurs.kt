package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException.ERROR_CODE_UNSPECIFIED
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenANoSuchElementExceptionOccurs {

	companion object {
		private var exoPlayerException: ExoPlayerException? = null
		private var eventListener: Player.Listener? = null
		private var playedFile: PlayedFile? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val mockExoPlayer = mockk<PromisingExoPlayer>()
			every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
			every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
			every { mockExoPlayer.getDuration() } returns 100L.toPromise()
			every { mockExoPlayer.removeListener(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.addListener(any()) } answers {
				eventListener = firstArg()
				mockExoPlayer.toPromise()
			}

			val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val promisedFuture = exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { it.promisePlayedFile() }
				.toExpiringFuture()
			eventListener?.onPlayerError(ExoPlaybackException.createForUnexpected(NoSuchElementException(), ERROR_CODE_UNSPECIFIED))
			try {
				playedFile = promisedFuture[1, TimeUnit.SECONDS]
			} catch (e: ExecutionException) {
				if (e.cause is ExoPlayerException) {
					exoPlayerException = e.cause as ExoPlayerException?
					return
				}
				throw e
			}
		}
	}

	@Test
	fun thenPlaybackCompletes() {
		assertThat(playedFile).isNotNull
	}

	@Test
	fun thenNoPlaybackErrorOccurs() {
		assertThat(exoPlayerException).isNull()
	}
}
