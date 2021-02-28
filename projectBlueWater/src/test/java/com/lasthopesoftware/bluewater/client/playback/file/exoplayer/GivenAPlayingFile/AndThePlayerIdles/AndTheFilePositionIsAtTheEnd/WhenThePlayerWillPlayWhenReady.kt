package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsAtTheEnd

import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenThePlayerWillPlayWhenReady {

	companion object {
		private var eventListener: Player.EventListener? = null
		private var isComplete = false
		@BeforeClass
		@Throws(InterruptedException::class, ExecutionException::class)
		fun before() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(100L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation: InvocationOnMock ->
				eventListener = invocation.getArgument(0)
				null
			}.`when`(mockExoPlayer).addListener(ArgumentMatchers.any())
			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val playbackPromise = exoPlayerPlaybackHandler.promisePlayback().eventually { obj -> obj.promisePlayedFile() }
				.then { isComplete = true }
			eventListener?.onPlayerStateChanged(true, Player.STATE_IDLE)
			try {
				FuturePromise(playbackPromise)[1, TimeUnit.SECONDS]
			} catch (ignored: TimeoutException) {
			}
		}
	}

	@Test
	fun thenPlaybackContinues() {
		AssertionsForClassTypes.assertThat(isComplete).isFalse
	}
}
