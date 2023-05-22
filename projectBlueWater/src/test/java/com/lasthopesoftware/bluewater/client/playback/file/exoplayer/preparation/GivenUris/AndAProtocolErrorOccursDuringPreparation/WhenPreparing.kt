package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndAProtocolErrorOccursDuringPreparation

import android.net.Uri
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
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
			{
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
			mockk(),
			mockk {
				every { promiseUri(LibraryId(libraryId), ServiceFile(1)) } returns Promise(mockk<Uri>())
			}
		)

		val futurePreparation = preparer.promisePreparedPlaybackFile(LibraryId(libraryId), ServiceFile(1), Duration.ZERO).toExpiringFuture()

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
