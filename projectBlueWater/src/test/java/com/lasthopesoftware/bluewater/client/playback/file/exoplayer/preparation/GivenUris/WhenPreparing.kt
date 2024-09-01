package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris

import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.BaseMediaSource
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue

private const val libraryId = 303

class WhenPreparing {

	private val preparedFile by lazy {
		val listeners = ConcurrentLinkedQueue<Player.Listener>()

		val preparer = ExoPlayerPlaybackPreparer(
			{ _, _ ->
				mockk<BaseMediaSource>(relaxUnitFun = true).toPromise()
			},
			mockk<ProvideExoPlayers>().apply {
				every { getExoPlayer() } returns mockk<PromisingExoPlayer>().apply {

					val selfPromise = this.toPromise()
					every { addListener(any()) } answers {
						listeners.add(firstArg())
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
						mockk(),
						firstArg(),
						secondArg()
					).toPromise()
				}
			},
			mockk {
				every { promiseUri(LibraryId(libraryId), ServiceFile(1)) } returns Promise(mockk<Uri>())
			}
		)
		val promisedPreparedFile = preparer.promisePreparedPlaybackFile(
			LibraryId(libraryId),
			ServiceFile(1),
			Duration.ZERO
		)

		listeners.forEach { it.onPlaybackStateChanged(Player.STATE_READY) }

		promisedPreparedFile.toExpiringFuture().get()
	}

	@Test
	fun `then an exo player is returned`() {
		assertThat(preparedFile!!.playbackHandler).isInstanceOf(
			ExoPlayerPlaybackHandler::class.java
		)
	}

	@Test
	fun `then a buffering file is returned`() {
		assertThat(preparedFile!!.bufferingPlaybackFile).isInstanceOf(
			BufferingExoPlayer::class.java
		)
	}
}
