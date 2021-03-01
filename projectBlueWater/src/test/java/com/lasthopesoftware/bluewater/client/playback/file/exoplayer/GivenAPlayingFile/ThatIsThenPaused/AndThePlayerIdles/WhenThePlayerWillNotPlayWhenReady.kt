package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.ThatIsThenPaused.AndThePlayerIdles

import com.annimon.stream.Stream
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenThePlayerWillNotPlayWhenReady {
	companion object {
		private val eventListeners: MutableCollection<Player.EventListener> = ArrayList()
		private val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class, ExecutionException::class)
		fun before() {
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation ->
				eventListeners.add(invocation.getArgument(0))
				mockExoPlayer.toPromise()
			}.`when`(mockExoPlayer).addListener(any())
			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
			playbackPromise
				.eventually { p ->
					val playableFilePromise = p.promisePause()
					Stream.of(eventListeners)
						.forEach { e -> e.onPlayerStateChanged(false, Player.STATE_IDLE) }
					playableFilePromise
				}
			val playedPromise = playbackPromise
				.eventually { obj -> obj.promisePlayedFile() }
			try {
				FuturePromise(playedPromise)[1, TimeUnit.SECONDS]
			} catch (ignored: TimeoutException) {
			}
		}
	}

	@Test
	fun thenPlaybackIsNotRestarted() {
		Mockito.verify(mockExoPlayer, Mockito.times(1)).setPlayWhenReady(true)
	}
}
