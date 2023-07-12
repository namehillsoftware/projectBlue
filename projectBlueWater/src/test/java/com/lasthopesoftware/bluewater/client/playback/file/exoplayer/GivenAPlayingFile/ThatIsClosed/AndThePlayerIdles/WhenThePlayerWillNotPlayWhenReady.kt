package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile.ThatIsClosed.AndThePlayerIdles

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

class WhenThePlayerWillNotPlayWhenReady {
	private val eventListeners: MutableCollection<Player.Listener> = ArrayList()
	private val mockExoPlayer = mockk<PromisingExoPlayer>(relaxed = true)

	@BeforeAll
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
		exoPlayerPlaybackHandler.promisePlayback().toExpiringFuture()[10, TimeUnit.SECONDS]
		exoPlayerPlaybackHandler.close()
		every { mockExoPlayer.getPlayWhenReady() } returns false.toPromise()
		eventListeners.forEach { e -> e.onPlaybackStateChanged(Player.STATE_IDLE) }
	}

	@Test
	fun `then playback is not restarted`() {
		verify(exactly = 1) { mockExoPlayer.setPlayWhenReady(true) }
	}
}
