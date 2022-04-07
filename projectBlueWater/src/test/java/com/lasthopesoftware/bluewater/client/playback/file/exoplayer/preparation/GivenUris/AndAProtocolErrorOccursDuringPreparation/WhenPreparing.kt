package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndAProtocolErrorOccursDuringPreparation

import android.net.Uri
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.Test
import java.net.ProtocolException

class WhenPreparing {

	companion object {
		private val preparedPlayer by lazy {
			var listener: Player.Listener? = null

			val preparer = ExoPlayerPlaybackPreparer(
				{
					mockk<BaseMediaSource>(relaxUnitFun = true)
				},
				mockk<ProvideExoPlayers>().apply {
					every { promiseExoPlayer() } returns Promise(mockk<PromisingExoPlayer>().apply {
						val player = this
						every { addListener(any()) } answers {
							listener = firstArg()
							player.toPromise()
						}
						every { setMediaSource(any()) } returns this.toPromise()
						every { removeListener(any()) } returns this.toPromise()
						every { prepare() } returns this.toPromise()
						every { stop() } returns this.toPromise()
						every { release() } returns this.toPromise()
					})
				},
				mockk()
			) { Promise(mockk<Uri>()) }

			val futurePreparation = preparer.promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO).toFuture()

			listener?.onPlayerError(
				PlaybackException(
					"oh no",
					HttpDataSource.HttpDataSourceException(
						ProtocolException("http://fool/"),
						DataSpec(mockk()),
						PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
						HttpDataSource.HttpDataSourceException.TYPE_OPEN
					),
					PlaybackException.ERROR_CODE_UNSPECIFIED,
				)
			)

			futurePreparation.get()
		}
	}

	@Test
	fun thenAnEmptyPlayerIsReturned() {
		assertThat(preparedPlayer).isInstanceOf(cls<EmptyPlaybackHandler>())
	}
}
