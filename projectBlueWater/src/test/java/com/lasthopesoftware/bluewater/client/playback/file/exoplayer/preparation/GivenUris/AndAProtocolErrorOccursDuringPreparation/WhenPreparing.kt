package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndAProtocolErrorOccursDuringPreparation

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.BaseMediaSource
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test
import java.net.ProtocolException
import java.util.concurrent.ExecutionException

private const val libraryId = 140

class WhenPreparing {

	private val exception by lazy {
		var listener: Player.Listener? = null

		val preparer = ExoPlayerPlaybackPreparer(
			{ _, _ ->
				mockk<BaseMediaSource>(relaxUnitFun = true).toPromise()
			},
			mockk<ProvideExoPlayers>().apply {
				every { getExoPlayer() } returns mockk<PromisingExoPlayer>().apply {
					val selfPromise = this.toPromise()
					every { addListener(any()) } answers {
						listener = firstArg()
						selfPromise
					}
					every { setMediaSource(any()) } returns selfPromise
					every { removeListener(any()) } returns selfPromise
					every { prepare() } returns selfPromise
					every { stop() } returns selfPromise
					every { release() } returns selfPromise
				}
			},
			mockk {
				every { promiseBufferingExoPlayer(any(), any()) } answers {
					BufferingExoPlayer(
                        mockk(),
                        firstArg(),
                        secondArg()
                    ).toPromise()
				}
			},
			mockk {
				every { promiseUri(LibraryId(libraryId), ServiceFile("1")) } returns Promise(mockk<Uri>())
			}
		)

		val futurePreparation = preparer.promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile("1"), Duration.ZERO).toExpiringFuture()

		listener?.onPlayerError(
			PlaybackException(
				"oh no",
				HttpDataSource.HttpDataSourceException(
					ProtocolException("royalty"),
					DataSpec(mockk()),
					PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
					HttpDataSource.HttpDataSourceException.TYPE_OPEN
				),
				PlaybackException.ERROR_CODE_UNSPECIFIED,
			)
		)

		try {
			futurePreparation.get()
			null
		} catch (e: ExecutionException) {
			e.cause
		}
	}

	@Test
	fun `then the root exception is thrown`() {
		assertThat(exception).isInstanceOf(cls<PlaybackException>())
	}
}
