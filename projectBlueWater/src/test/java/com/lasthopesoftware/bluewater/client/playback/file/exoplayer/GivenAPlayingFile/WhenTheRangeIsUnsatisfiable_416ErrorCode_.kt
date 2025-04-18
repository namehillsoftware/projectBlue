package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.GivenAPlayingFile

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class WhenTheRangeIsUnsatisfiable_416ErrorCode_ {

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

		val uri = Uri.EMPTY
		eventListeners.forEach {
			it.onPlayerError(
				ExoPlaybackException.createForSource(
					InvalidResponseCodeException(
						416,
						"",
						IOException(),
						HashMap(),
						DataSpec(uri),
						ByteArray(0)
					),
					PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
				)
			)
		}

		promisedFuture[1, TimeUnit.SECONDS]
	}

	@Test
	fun `then playback completes`() {
		assertThat(playedFile).isNotNull
	}
}
