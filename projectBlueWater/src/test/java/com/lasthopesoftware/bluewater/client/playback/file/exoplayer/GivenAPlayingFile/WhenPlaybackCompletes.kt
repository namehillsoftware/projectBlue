package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import androidx.media3.common.Player
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenPlaybackCompletes {

	private val eventListeners: MutableList<Player.Listener> = ArrayList()
	private val playedFile by lazy {
		val mockExoPlayer = mockk<PromisingExoPlayer>()
		every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.removeListener(any()) } returns mockExoPlayer.toPromise()
		every { mockExoPlayer.addListener(any()) } answers {
			eventListeners.add(firstArg())
			mockExoPlayer.toPromise()
		}

		val playbackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)

		val playedFileFuture =
			playbackHandler
				.promisePlayback()
				.eventually { obj -> obj.promisePlayedFile() }
				.toExpiringFuture()

		eventListeners.forEach { e -> e.onPlaybackStateChanged(Player.STATE_ENDED) }

		playedFileFuture[10, TimeUnit.SECONDS]
	}

	@Test
	fun `then the played file is returned`() {
		assertThat(playedFile).isNotNull
	}
}
