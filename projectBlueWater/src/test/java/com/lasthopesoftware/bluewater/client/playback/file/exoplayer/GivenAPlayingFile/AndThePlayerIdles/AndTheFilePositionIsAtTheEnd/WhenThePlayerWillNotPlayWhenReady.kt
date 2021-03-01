package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsAtTheEnd

import com.annimon.stream.Stream
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
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
		private var isComplete = false

		@JvmStatic
		@BeforeClass
		@Throws(InterruptedException::class, TimeoutException::class, ExecutionException::class)
		fun before() {
			val mockExoPlayer = Mockito.mock(PromisingExoPlayer::class.java)
			Mockito.`when`(mockExoPlayer.getPlayWhenReady()).thenReturn(true.toPromise())
			Mockito.`when`(mockExoPlayer.getCurrentPosition()).thenReturn(100L.toPromise())
			Mockito.`when`(mockExoPlayer.getDuration()).thenReturn(100L.toPromise())
			Mockito.doAnswer { invocation ->
				eventListeners.add(invocation.getArgument(0))
			}.`when`(mockExoPlayer).addListener(any())

			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
				.eventually { obj -> obj.promisePlayedFile() }
				.then { isComplete = true }

			Stream.of(eventListeners).forEach { e -> e.onPlayerStateChanged(false, Player.STATE_IDLE) }
			FuturePromise(playbackPromise)[1, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenPlaybackCompletes() {
		AssertionsForClassTypes.assertThat(isComplete).isTrue
	}
}
