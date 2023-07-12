package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.AndThePlayerIdles.AndTheFilePositionIsNotAtTheEnd

import androidx.media3.common.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class WhenThePlayerWillPlayWhenReady {
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

	@BeforeAll
	fun act() {
		val exoPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
		val playbackPromise = exoPlayerPlaybackHandler.promisePlayback()
			.eventually { it.promisePlayedFile() }
			.toExpiringFuture()
		eventListeners.forEach { e -> e.onPlaybackStateChanged(Player.STATE_IDLE) }
		try {
			playbackPromise[1, TimeUnit.SECONDS]
		} catch (ignored: TimeoutException) {
		}
	}

	@Test
	fun `then playback is not restarted`() {
		verify(exactly = 1) { mockExoPlayer.setPlayWhenReady(true) }
	}
}
