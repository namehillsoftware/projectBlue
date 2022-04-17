package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.fakes.ResolvablePlaybackHandler
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenPlaybackIsPausedAndRestarted {
	companion object {
		private val changedFiles: MutableList<ServiceFile> = ArrayList()
		private var playbackEngine: PlaybackEngine? = null
		private var nowPlaying: NowPlaying? = null
		private var resolveablePlaybackHandler: ResolvablePlaybackHandler? = null
		private var playbackStartedCount = 0

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			val library = Library()
			library.setId(1)
			val libraryProvider = PassThroughSpecificLibraryProvider(library)
			val libraryStorage = PassThroughLibraryStorage()
			val nowPlayingRepository =
                NowPlayingRepository(
                    libraryProvider,
                    libraryStorage
                )
			playbackEngine = PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(
					fakePlaybackPreparerProvider
				) { 1 }, listOf(CompletingFileQueueProvider()),
				nowPlayingRepository,
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
			playbackEngine
				?.setOnPlaybackStarted { ++playbackStartedCount }
				?.setOnPlayingFileChanged { f -> changedFiles.add(f.serviceFile) }
			playbackEngine
				?.startPlaylist(
					listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					), 0, Duration.ZERO
				)
			val playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
			resolveablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
			playingPlaybackHandler.resolve()
			resolveablePlaybackHandler?.setCurrentPosition(30)
			playbackEngine?.pause()?.toExpiringFuture()?.get()
			nowPlaying = nowPlayingRepository.promiseNowPlaying().toExpiringFuture().get()
			playbackEngine?.resume()?.toExpiringFuture()?.get()
			nowPlaying = nowPlayingRepository.promiseNowPlaying().toExpiringFuture().get()
		}
	}

	@Test
	fun thenThePlayerIsNotPlaying() {
		assertThat(resolveablePlaybackHandler!!.isPlaying).isTrue
	}

	@Test
	fun thenThePlaybackStateIsNotPlaying() {
		assertThat(playbackEngine!!.isPlaying).isTrue
	}

	@Test
	fun thenTheSavedFilePositionIsCorrect() {
		assertThat(nowPlaying!!.filePosition).isEqualTo(30)
	}

	@Test
	fun thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(1)
	}

	@Test
	fun thenTheChangedFilesAreCorrect() {
		assertThat(changedFiles).containsExactly(
			ServiceFile(1),
			ServiceFile(2),
			ServiceFile(2)
		)
	}

	@Test
	fun thenPlaybackHasBeenStartedTwice() {
		assertThat(playbackStartedCount).isEqualTo(2)
	}

	@Test
	fun thenTheSavedPlaylistIsCorrect() {
		assertThat(nowPlaying!!.playlist)
			.containsExactly(
				ServiceFile(1),
				ServiceFile(2),
				ServiceFile(3),
				ServiceFile(4),
				ServiceFile(5)
			)
	}
}
