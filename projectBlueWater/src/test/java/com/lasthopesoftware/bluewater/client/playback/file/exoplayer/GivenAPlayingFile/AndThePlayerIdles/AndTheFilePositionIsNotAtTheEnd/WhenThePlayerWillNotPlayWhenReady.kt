package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsNotAtTheEnd

import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenThePlayerWillNotPlayWhenReady {

	companion object {
		private var eventListener: Player.Listener? = null
		private val playedFile by lazy {
			val mockExoPlayer = mockk<PromisingExoPlayer>()
			every { mockExoPlayer.getPlayWhenReady() } returns false.toPromise()
			every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.removeListener(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
			every { mockExoPlayer.getDuration() } returns 100L.toPromise()
			every { mockExoPlayer.addListener(any()) } answers {
				eventListener = firstArg()
				mockExoPlayer.toPromise()
			}

			val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
				.eventually { it.promisePlayedFile() }
				.toFuture()

			eventListener?.onPlaybackStateChanged(Player.STATE_IDLE)
			playbackPromise[10, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenPlaybackCompletes() {
		assertThat(playedFile).isNotNull
	}
}
