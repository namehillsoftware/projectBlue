package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 976

class WhenObservingPlayback {

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library()
		library.setId(libraryId)
		val libraryProvider = FakeLibraryRepository(library)
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
			listOf(CompletingFileQueueProvider()),
			NowPlayingRepository(
				libraryProvider,
				libraryProvider,
				FakeNowPlayingState(),
			),
			PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
		)

		Pair(fakePlaybackPreparerProvider, playbackEngine)
	}

	private var firstSwitchedFile: PositionedPlayingFile? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, playbackEngine) = mut

		playbackEngine
			.setOnPlayingFileChanged { _, p -> firstSwitchedFile = p }
			.startPlaylist(
				LibraryId(libraryId),
				listOf(
					ServiceFile(1),
					ServiceFile(2),
					ServiceFile(3),
					ServiceFile(4),
					ServiceFile(5)
				), 0, Duration.ZERO
			)
		fakePlaybackPreparerProvider.deferredResolution.resolve()
	}

	@Test
	fun `then the first track is broadcast`() {
		assertThat(firstSwitchedFile!!.playlistPosition).isEqualTo(0)
	}
}
