package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.annimon.stream.Stream
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import java.net.ProtocolException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WhenANormalProtocolExceptionOccurs {

	companion object {
		private var exoPlayerException: ExoPlayerException? = null
		private val eventListener: MutableList<Player.EventListener> = ArrayList()

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class)
		fun context() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation: InvocationOnMock ->
				eventListener.add(invocation.getArgument(0))
				mockExoPlayer.toPromise()
			}.`when`(mockExoPlayer).addListener(any())
			val countDownLatch = CountDownLatch(1)
			val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { obj: PlayingFile -> obj.promisePlayedFile() }
				.then({ null }
				) { e ->
					if (e is ExoPlayerException) {
						exoPlayerException = e
					}
					null
				}
				.then {
					countDownLatch.countDown()
				}
			Stream.of(eventListener).forEach { e -> e.onPlayerError(ExoPlaybackException.createForSource(ProtocolException())) }
			countDownLatch.await(1, TimeUnit.SECONDS)
		}
	}

	@Test
	fun thenThePlaybackErrorIsCorrect() {
		AssertionsForClassTypes.assertThat(exoPlayerException!!.cause).isInstanceOf(ExoPlaybackException::class.java)
	}
}
