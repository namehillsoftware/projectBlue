package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsAtTheEnd

import androidx.media3.common.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenThePlayerWillPlayWhenReady {

	private var eventListener: Player.Listener? = null
	private val playedFile by lazy {
		val mockExoPlayer = mockk<PromisingExoPlayer>()
		every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
		every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.removeListener(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.getCurrentPosition() } returns 100L.toPromise()
		every { mockExoPlayer.getDuration() } returns 100L.toPromise()
		every { mockExoPlayer.addListener(any()) } answers {
			eventListener = firstArg()
			mockExoPlayer.toPromise()
		}

		val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
		val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
			.eventually { it.promisePlayedFile() }
			.toExpiringFuture()
		eventListener?.onPlaybackStateChanged(Player.STATE_IDLE)

		try {
			playbackPromise[1, TimeUnit.SECONDS]
		} catch (ignored: TimeoutException) {
			null
		}
	}

	@Test
	fun `then playback does not continue`() {
		assertThat(playedFile).isNull()
	}
}
