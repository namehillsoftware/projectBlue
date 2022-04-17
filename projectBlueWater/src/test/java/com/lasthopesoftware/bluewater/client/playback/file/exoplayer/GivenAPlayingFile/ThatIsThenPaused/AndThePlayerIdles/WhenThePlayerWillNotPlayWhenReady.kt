package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.ThatIsThenPaused.AndThePlayerIdles

import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenThePlayerWillNotPlayWhenReady {
	companion object {
		private val eventListeners: MutableCollection<Player.Listener> = ArrayList()
		private val mockExoPlayer = mockk<PromisingExoPlayer>()

		@JvmStatic
		@BeforeClass
		fun before() {
			every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
			every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
			every { mockExoPlayer.getDuration() } returns 100L.toPromise()
			every { mockExoPlayer.addListener(any()) } answers {
				eventListeners.add(firstArg())
				mockExoPlayer.toPromise()
			}

			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
			playbackPromise
				.eventually { p ->
					val playableFilePromise = p.promisePause()
					every { mockExoPlayer.getPlayWhenReady() } returns false.toPromise()
					eventListeners.forEach { e -> e.onPlaybackStateChanged(Player.STATE_IDLE) }
					playableFilePromise
				}
			val playedPromise = playbackPromise
				.eventually { obj -> obj.promisePlayedFile() }
			try {
				ExpiringFuturePromise(playedPromise)[1, TimeUnit.SECONDS]
			} catch (ignored: TimeoutException) {
			}
		}
	}

	@Test
	fun thenPlaybackIsNotRestarted() {
		verify(exactly = 1) { mockExoPlayer.setPlayWhenReady(true) }
	}
}
