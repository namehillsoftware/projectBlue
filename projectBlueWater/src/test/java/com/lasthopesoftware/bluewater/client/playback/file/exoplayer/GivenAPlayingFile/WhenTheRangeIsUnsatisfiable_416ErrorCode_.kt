package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import android.net.Uri
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.PlayedFile
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class WhenTheRangeIsUnsatisfiable_416ErrorCode_ {

	companion object {
		private var playedFile: PlayedFile? = null
		private var eventListener: Player.Listener? = null

		@JvmStatic
		@BeforeClass
		fun before() {
			val mockExoPlayer = mockk<PromisingExoPlayer>(relaxed = true)
			every { mockExoPlayer.setPlayWhenReady(any()) } returns mockExoPlayer.toPromise()
			every { mockExoPlayer.getPlayWhenReady() } returns true.toPromise()
			every { mockExoPlayer.getCurrentPosition() } returns 50L.toPromise()
			every { mockExoPlayer.getDuration() } returns 100L.toPromise()
			every { mockExoPlayer.addListener(any()) } answers {
				eventListener = firstArg()
				mockExoPlayer.toPromise()
			}

			val exoPlayerPlaybackHandlerPlayerPlaybackHandler = ExoPlayerPlaybackHandler(mockExoPlayer)
			val promisedFuture = exoPlayerPlaybackHandlerPlayerPlaybackHandler.promisePlayback()
				.eventually { obj -> obj.promisePlayedFile() }
				.toFuture()

			eventListener?.onPlayerError(ExoPlaybackException.createForSource(
				InvalidResponseCodeException(416, "", IOException(), HashMap(), DataSpec(Uri.EMPTY), ByteArray(0)),
				PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))

			playedFile = promisedFuture[1, TimeUnit.SECONDS]
		}
	}

	@Test
	fun thenPlaybackCompletes() {
		assertThat(playedFile).isNotNull
	}
}
