package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.GivenUris

import android.net.Uri
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.BaseMediaSource
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.exoplayer.PromisingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.exoplayer.ProvideExoPlayers
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.ExoPlayerPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.buffering.BufferingExoPlayer
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.ExoPlayerPlaybackPreparer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

private val preparedFile by lazy {
	val listeners = ConcurrentLinkedQueue<Player.Listener>()

	val preparer = ExoPlayerPlaybackPreparer(
		{
			mockk<BaseMediaSource>(relaxUnitFun = true)
		},
		mockk<ProvideExoPlayers>().apply {
			every { promiseExoPlayer() } returns Promise(mockk<PromisingExoPlayer>().apply {

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
			})
		},
		mockk()
	) { Promise(mockk<Uri>()) }
	val promisedPreparedFile = preparer.promisePreparedPlaybackFile(
		ServiceFile(1),
		Duration.ZERO
	)

	listeners.forEach { it.onPlaybackStateChanged(Player.STATE_READY) }

	promisedPreparedFile.toFuture().get()
}

class WhenPreparing {

	@Test
	fun thenAnExoPlayerIsReturned() {
		assertThat(preparedFile!!.playbackHandler).isInstanceOf(
			ExoPlayerPlaybackHandler::class.java
		)
	}

	@Test
	fun thenABufferingFileIsReturned() {
		assertThat(preparedFile!!.bufferingPlaybackFile).isInstanceOf(
			BufferingExoPlayer::class.java
		)
	}
}
