package com.lasthopesoftware.bluewater.client.playback.playlist.GivenAStandardPreparedPlaylistProvider.WithAStatefulPlaybackHandler.ThatIsPlaying

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.SupplyQueuedPreparedFiles
import com.lasthopesoftware.bluewater.client.playback.file.NoTransformVolumeManager
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile
import com.lasthopesoftware.bluewater.client.playback.file.fakes.FakeBufferingPlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.playlist.PlaylistPlayer
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Halting Playback` {

	private val affectedSystems by lazy {
		FakeBufferingPlaybackHandler()
	}
	private val mut by lazy {
		val preparedPlaybackFileQueue = mockk<SupplyQueuedPreparedFiles> {
			every { promiseNextPreparedPlaybackFile(Duration.ZERO) } returns PositionedPlayableFile(
				0,
				affectedSystems,
				NoTransformVolumeManager(),
				ServiceFile(1)
			).toPromise()
		}
		PlaylistPlayer(preparedPlaybackFileQueue, Duration.ZERO)
	}

	private var isCompleted = false

	@BeforeAll
	fun act() {
		val playlistPlayback = mut
		val disposable = Observable.create(playlistPlayback).subscribe(
			{},
			{},
			{ isCompleted = true }
		)
		playlistPlayback.haltPlayback().toExpiringFuture().get()
		disposable.dispose()
	}

	@Test
	fun `then the playable file is closed`() {
		assertThat(affectedSystems.isClosed).isTrue
	}

	@Test
	fun `then the player feed is not completed`() {
		assertThat(isCompleted).isFalse
	}
}