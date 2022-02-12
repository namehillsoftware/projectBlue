package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaylistEngine.AndAPlaylistIsPreparing

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.Test

class WhenATrackIsSwitchedTwice {
	companion object {
		private val nextSwitchedFile by lazy {
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
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f)))

			playbackEngine
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
			playbackEngine.changePosition(3, Duration.ZERO).toFuture()

			val futurePlaylist = playbackEngine.changePosition(4, Duration.ZERO).toFuture()

			fakePlaybackPreparerProvider.deferredResolution.resolve()

			futurePlaylist.get()
		}
	}

	@Test
	fun thenTheNextFileChangeIsTheSwitchedToTheCorrectTrackPosition() {
		assertThat(nextSwitchedFile?.playlistPosition).isEqualTo(4)
	}
}
