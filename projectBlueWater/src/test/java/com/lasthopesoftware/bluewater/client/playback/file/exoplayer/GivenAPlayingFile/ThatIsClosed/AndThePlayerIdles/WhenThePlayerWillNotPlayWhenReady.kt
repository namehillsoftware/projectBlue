package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.ThatIsClosed.AndThePlayerIdles

import com.annimon.stream.Stream
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
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
		@Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
		fun before() {
			Mockito.`when`(mockExoPlayer.setPlayWhenReady(anyBoolean())).thenReturn(mockExoPlayer.toPromise())
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation ->
				eventListeners.add(invocation.getArgument(0))
				null
			}.`when`(mockExoPlayer).addListener(any())
			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			FuturePromise(exoPlayerPlaybackHandler.promisePlayback())[1, TimeUnit.SECONDS]
			exoPlayerPlaybackHandler.close()
			Stream.of(eventListeners)
				.forEach { e -> e.onPlayerStateChanged(false, Player.STATE_IDLE) }
		}
	}

	@Test
	fun thenPlaybackIsNotRestarted() {
		Mockito.verify(mockExoPlayer, Mockito.times(1)).setPlayWhenReady(true)
	}
}
