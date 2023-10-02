package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenResumingPlayback {

	private val playbackHandler = FakeBufferingPlaybackHandler()
	private var illegalStateException: IllegalStateException? = null
	private var playingFile: PositionedPlayingFile? = null

	@BeforeAll
	fun act() {
		val positionedPlaybackHandlerContainer = Promise(PositionedPlayableFile(
			0,
			playbackHandler,
			NoTransformVolumeManager(),
			ServiceFile(1)))
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles> {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns positionedPlaybackHandlerContainer
		}
		val playlistPlayback = PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
		Observable.create(playlistPlayback).subscribe()
		playlistPlayback.resume().toExpiringFuture().get()

		try {
			playingFile = playlistPlayback.resume().toExpiringFuture().get()
		} catch (e: ExecutionException) {
			illegalStateException = e.cause as? IllegalStateException
		}
	}

	@Test
	fun `then an illegal state exception is thrown as it is illegal to resume many times`() {
		assertThat(illegalStateException).isNotNull
	}
}
