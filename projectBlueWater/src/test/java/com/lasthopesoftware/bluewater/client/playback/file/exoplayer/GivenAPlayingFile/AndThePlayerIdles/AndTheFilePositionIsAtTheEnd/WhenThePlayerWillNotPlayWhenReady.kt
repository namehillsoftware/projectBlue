package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsAtTheEnd

import com.google.android.exoplayer2.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class WhenThePlayerWillNotPlayWhenReady {

	private val eventListeners: MutableCollection<Player.Listener> = ArrayList()
	private val playedFile by lazy {
		val mockExoPlayer = mockk<PromisingExoPlayer>()
		every { mockExoPlayer.getPlayWhenReady() } returns false.toPromise()
		every { mockExoPlayer.setPlayWhenReady(true) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.removeListener(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.getCurrentPosition() } returns 100L.toPromise()
		every { mockExoPlayer.getDuration() } returns 100L.toPromise()
		every { mockExoPlayer.addListener(any()) } answers {
			eventListeners.add(firstArg())
			mockExoPlayer.toPromise()
		}

		val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
		val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
			.eventually { obj -> obj.promisePlayedFile() }
			.toExpiringFuture()

		eventListeners.forEach { e -> e.onPlaybackStateChanged(Player.STATE_IDLE) }
		playbackPromise[10, TimeUnit.SECONDS]
	}

	@Test
	fun `then playback completes`() {
		assertThat(playedFile).isNotNull
	}
}
