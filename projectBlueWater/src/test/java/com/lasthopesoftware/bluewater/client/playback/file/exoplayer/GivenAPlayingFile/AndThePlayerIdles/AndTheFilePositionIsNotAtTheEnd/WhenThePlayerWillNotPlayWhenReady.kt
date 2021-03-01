package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsNotAtTheEnd

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
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenThePlayerWillNotPlayWhenReady {

	companion object {
		private var eventListener: Player.EventListener? = null
		private var isComplete = false

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class, ExecutionException::class)
		fun before() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(50L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation: InvocationOnMock ->
				eventListener = invocation.getArgument(0)
				mockExoPlayer.toPromise()
			}.`when`(mockExoPlayer).addListener(any())

			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val playbackPromise = exoPlayerPlaybackHandler.promisePlayback().eventually { obj: PlayingFile -> obj.promisePlayedFile() }
				.then { isComplete = true }

			eventListener?.onPlayerStateChanged(false, Player.STATE_IDLE)
			try {
				FuturePromise(playbackPromise)[1, TimeUnit.SECONDS]
			} catch (ignored: TimeoutException) {
			}
		}
	}

	@Test
	fun thenPlaybackCompletes() {
		AssertionsForClassTypes.assertThat(isComplete).isTrue
	}
}
