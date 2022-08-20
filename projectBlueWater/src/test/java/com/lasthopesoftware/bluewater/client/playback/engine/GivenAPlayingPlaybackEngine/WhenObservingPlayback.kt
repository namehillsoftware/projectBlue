package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenObservingPlayback {

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library()
		library.setId(1)
		val libraryProvider = PassThroughSpecificLibraryProvider(library)
		val libraryStorage = PassThroughLibraryStorage()
		val playbackEngine = PlaybackEngine(
			PreparedPlaybackQueueResourceManagement(
				fakePlaybackPreparerProvider
			) { 1 }, listOf(CompletingFileQueueProvider()),
			NowPlayingRepository(
				libraryProvider,
				libraryStorage
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
			.setOnPlayingFileChanged { p -> firstSwitchedFile = p }
			.startPlaylist(
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
