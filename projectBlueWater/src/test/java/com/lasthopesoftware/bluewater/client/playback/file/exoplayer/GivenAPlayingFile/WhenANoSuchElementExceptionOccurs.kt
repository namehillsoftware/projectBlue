package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import androidx.media3.common.PlaybackException.ERROR_CODE_UNSPECIFIED
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlaybackException
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.error.ExoPlayerException
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class WhenANoSuchElementExceptionOccurs {

	private var exoPlayerException: ExoPlayerException? = null
	private var playedFile: PlayedFile? = null

	@BeforeAll
	fun act() {
		val eventListeners = ArrayList<Player.Listener>()

		val mockExoPlayer = mockk<PromisingExoPlayer>()
		every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
		every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
		every { mockExoPlayer.getDuration() } returns 100L.toPromise()
		every { mockExoPlayer.removeListener(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.addListener(any()) } answers {
			eventListeners.add(firstArg())
			mockExoPlayer.toPromise()
		}

		val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
		val promisedFuture = exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
			.eventually { it.promisePlayedFile() }
			.toExpiringFuture()
		eventListeners.forEach { it.onPlayerError(ExoPlaybackException.createForUnexpected(NoSuchElementException(), ERROR_CODE_UNSPECIFIED)) }
		try {
			playedFile = promisedFuture[1, TimeUnit.SECONDS]
		} catch (e: ExecutionException) {
			if (e.cause is ExoPlayerException) {
				exoPlayerException = e.cause as ExoPlayerException?
				return
			}
			throw e
		}
	}

	@Test
	fun `then playback completes`() {
		assertThat(playedFile).isNotNull
	}

	@Test
	fun `then no playback error occurs`() {
		assertThat(exoPlayerException).isNull()
	}
}
