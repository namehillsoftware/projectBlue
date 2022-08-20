package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris.AndAProtocolErrorOccursDuringPreparation.AndItIsDueToEndOfStream

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
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test
import java.net.ProtocolException

class WhenPreparing {

	private val preparedPlayer by lazy {
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
			mockk()
		) { Promise(mockk<Uri>()) }

		val futurePreparation = preparer.promisePreparedPlaybackFile(ServiceFile(1), Duration.ZERO).toExpiringFuture()

		listener?.onPlayerError(
			PlaybackException(
				"oh no",
				HttpDataSource.HttpDataSourceException(
					ProtocolException("unexpected end of stream"),
					DataSpec(mockk()),
					PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
					HttpDataSource.HttpDataSourceException.TYPE_OPEN
				),
				PlaybackException.ERROR_CODE_UNSPECIFIED,
			)
		)

		futurePreparation.get()
	}

	@Test
	fun `then an empty player is returned`() {
		assertThat(preparedPlayer?.playbackHandler).isInstanceOf(cls<EmptyPlaybackHandler>())
	}
}
