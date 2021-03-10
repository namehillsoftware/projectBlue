package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import com.annimon.stream.Stream
import com.google.android.exoplayer2.Player
import com.lasthopesoftware.any
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.*
import java.util.*
import java.util.concurrent.TimeUnit

class WhenPlaybackCompletes {

	companion object {
		private val eventListeners: MutableList<Player.EventListener> = ArrayList()
		private var playedFile: PlayedFile? = null

		@BeforeClass
		@JvmStatic
		fun context() {
			val mockExoPlayer = mock(PromisingExoPlayer::class.java)
			`when`(mockExoPlayer.setPlayWhenReady(anyBoolean())).thenReturn(mockExoPlayer.toPromise())
			doAnswer { invocation ->
				eventListeners.add(invocation.getArgument(0))
				mockExoPlayer.toPromise()
			}.`when`(mockExoPlayer).addListener(any())
			val playbackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)

			val playedFileFuture =
				playbackHandler
					.promisePlayback()
					.eventually { obj -> obj.promisePlayedFile() }
					.toFuture()

			Stream.of(eventListeners).forEach { e: Player.EventListener -> e.onPlayerStateChanged(false, Player.STATE_ENDED) }

			playedFile = playedFileFuture[1, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenThePlayedFileIsReturned() {
		AssertionsForClassTypes.assertThat(playedFile).isNotNull
	}
}
