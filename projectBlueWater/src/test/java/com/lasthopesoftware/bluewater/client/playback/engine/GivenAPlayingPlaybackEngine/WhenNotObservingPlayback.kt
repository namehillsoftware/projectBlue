package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.ISpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine.Companion.createEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.joda.time.Duration
import org.junit.Test
import java.util.*

class WhenNotObservingPlayback {

	companion object {
		private val library = Library(_id = 1, _nowPlayingId = 5)
		private val playbackEngine by lazy {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()

			val libraryProvider = mockk<ISpecificLibraryProvider>()
			every { libraryProvider.library } returns Promise(library)

			val libraryStorage = mockk<ILibraryStorage>()
			every { libraryStorage.saveLibrary(any()) } returns	Promise(library)

			val playbackEngine = createEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 }, listOf(CompletingFileQueueProvider()),
					NowPlayingRepository(libraryProvider, libraryStorage),
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				).toFuture().get()
			playbackEngine
				?.startPlaylist(
					Arrays.asList(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					), 0, Duration.ZERO
				)
			val resolveablePlaybackHandler =
				fakePlaybackPreparerProvider.deferredResolution.resolve()
			fakePlaybackPreparerProvider.deferredResolution.resolve()
			resolveablePlaybackHandler.resolve()
			playbackEngine
		}
	}

	@Test
	fun thenTheSavedTrackPositionIsOne() {
		Assertions.assertThat(library.nowPlayingId).isEqualTo(1)
	}

	@Test
	fun thenTheManagerIsPlaying() {
		Assertions.assertThat(playbackEngine?.isPlaying).isTrue
	}
}
