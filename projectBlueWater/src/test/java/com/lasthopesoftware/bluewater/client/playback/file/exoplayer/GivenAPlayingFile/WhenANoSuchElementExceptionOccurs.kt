package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenANoSuchElementExceptionOccurs {

	companion object {
		private var exoPlayerException: ExoPlayerException? = null
		private var eventListener: Player.EventListener? = null
		private var isComplete: Boolean? = null
		@BeforeClass
		@Throws(InterruptedException::class, TimeoutException::class, ExecutionException::class)
		fun before() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation: InvocationOnMock ->
				eventListener = invocation.getArgument(0)
				null
			}.`when`(mockExoPlayer).addListener(ArgumentMatchers.any())
			val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val promisedFuture = exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { obj -> obj.promisePlayedFile() }
				.then({ true }) { false }.toFuture()
			eventListener!!.onPlayerError(ExoPlaybackException.createForUnexpected(NoSuchElementException()))
			try {
				isComplete = promisedFuture[1, TimeUnit.SECONDS]
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
		AssertionsForClassTypes.assertThat(isComplete).isTrue
	}

	@Test
	fun thenNoPlaybackErrorOccurs() {
		AssertionsForClassTypes.assertThat(exoPlayerException).isNull()
	}
}
