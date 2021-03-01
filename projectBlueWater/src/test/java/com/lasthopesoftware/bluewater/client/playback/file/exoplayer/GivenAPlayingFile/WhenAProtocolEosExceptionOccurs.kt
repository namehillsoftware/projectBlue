package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import java.net.ProtocolException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenAProtocolEosExceptionOccurs {

	companion object {
		private var exoPlayerException: ProtocolException? = null
		private var eventListener: Player.EventListener? = null
		private var isComplete: Boolean? = null

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
		fun before() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation: InvocationOnMock ->
				eventListener = invocation.getArgument(0)
				mockExoPlayer.toPromise()
			}.`when`(mockExoPlayer).addListener(any())
			val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val promisedFuture = FuturePromise(exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { obj: PlayingFile -> obj.promisePlayedFile() }
				.then({ true }) { false })
			eventListener!!.onPlayerError(ExoPlaybackException.createForSource(ProtocolException("unexpected end of stream")))
			try {
				isComplete = promisedFuture[1, TimeUnit.SECONDS]
			} catch (e: ExecutionException) {
				if (e.cause is ProtocolException) {
					exoPlayerException = e.cause as ProtocolException?
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
