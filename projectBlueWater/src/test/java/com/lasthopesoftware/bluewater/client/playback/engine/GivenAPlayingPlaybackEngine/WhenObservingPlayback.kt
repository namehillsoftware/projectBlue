package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine.Companion.createEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenObservingPlayback {
	@Test
	fun thenTheFirstTrackIsBroadcast() {
		assertThat(firstSwitchedFile!!.playlistPosition).isEqualTo(0)
	}

	companion object {
		private var firstSwitchedFile: PositionedPlayingFile? = null

		@BeforeClass
		@JvmStatic
		fun context() {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			val library = Library()
			library.setId(1)
			val libraryProvider = PassThroughSpecificLibraryProvider(library)
			val libraryStorage = PassThroughLibraryStorage()
			val playbackEngine = FuturePromise(
				createEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 }, listOf(CompletingFileQueueProvider()),
					NowPlayingRepository(libraryProvider, libraryStorage),
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				)
			).get()
			playbackEngine
				?.setOnPlayingFileChanged { p -> firstSwitchedFile = p }
				?.startPlaylist(
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
	}
}
