package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlaybackException
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.ProtocolException

class WhenAProtocolEosExceptionOccurs {

	private val playedFile by lazy {
		val eventListeners = ArrayList<Player.Listener>()
		val mockExoPlayer = mockk<PromisingExoPlayer>(relaxed = true)
		every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
		every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
		every { mockExoPlayer.getDuration() } returns 100L.toPromise()
		every { mockExoPlayer.addListener(any()) } answers {
			eventListeners.add(firstArg())
			mockExoPlayer.toPromise()
		}

		val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
		val promisedFuture = exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
			.eventually { obj -> obj.promisePlayedFile() }
			.toExpiringFuture()
		eventListeners.forEach {
			it.onPlayerError(
				ExoPlaybackException.createForSource(
					ProtocolException("unexpected end of stream"),
					PlaybackException.ERROR_CODE_IO_UNSPECIFIED
				)
			)
		}

		promisedFuture.get()
	}

	@Test
	fun `then playback completes`() {
		assertThat(playedFile).isNotNull
	}
}
