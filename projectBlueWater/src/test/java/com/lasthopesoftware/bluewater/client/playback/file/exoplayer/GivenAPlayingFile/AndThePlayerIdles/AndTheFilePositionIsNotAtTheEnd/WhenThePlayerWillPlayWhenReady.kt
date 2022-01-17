package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsNotAtTheEnd

import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenThePlayerWillPlayWhenReady {
	companion object {
		private val eventListeners: MutableCollection<Player.Listener> = ArrayList()
		private val mockExoPlayer by lazy {
			val mockExoPlayer = mockk<PromisingExoPlayer>()
			every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
			every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.removeListener(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
			every { mockExoPlayer.getDuration() } returns 100L.toPromise()
			every { mockExoPlayer.addListener(any()) } answers {
				eventListeners.add(firstArg())
				mockExoPlayer.toPromise()
			}
			mockExoPlayer
		}

		@JvmStatic
		@BeforeClass
		fun before() {
			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
				.eventually { it.promisePlayedFile() }
				.toFuture()
			eventListeners.forEach { e -> e.onPlaybackStateChanged(Player.STATE_IDLE) }
			try {
				playbackPromise[1, TimeUnit.SECONDS]
			} catch (ignored: TimeoutException) {
			}
		}
 	}

	@Test
	fun thenPlaybackIsNotRestarted() {
		verify(exactly = 1) { mockExoPlayer.setPlayWhenReady(true) }
	}
}
