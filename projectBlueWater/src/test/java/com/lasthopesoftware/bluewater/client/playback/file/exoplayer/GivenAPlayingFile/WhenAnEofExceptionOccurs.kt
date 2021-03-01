package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import java.io.EOFException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WhenAnEofExceptionOccurs {

	companion object {
		private var exoPlayerException: ExoPlayerException? = null
		private var eventListener: Player.EventListener? = null
		private var isComplete = false

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class)
		fun before() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation: InvocationOnMock ->
				eventListener = invocation.getArgument(0)
				mockExoPlayer.toPromise()
			}.`when`(mockExoPlayer).addListener(any())
			val countDownLatch = CountDownLatch(1)
			val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { obj -> obj.promisePlayedFile() }
				.then(
					{ isComplete = true },
					{ e ->
						if (e is ExoPlayerException) {
							exoPlayerException = e
						}
						isComplete = false
					})
				.then {
					countDownLatch.countDown()
					null
				}
			eventListener!!.onPlayerError(ExoPlaybackException.createForSource(EOFException()))
			countDownLatch.await(1, TimeUnit.SECONDS)
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
