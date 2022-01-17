package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughLibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.PassThroughSpecificLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.view.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenPlaybackIsPausedAndPositionIsChangedAndRestarted {
	companion object {
		private var playbackEngine: PlaybackEngine? = null
		private var nowPlaying: NowPlaying? = null
		private val positionedFiles: MutableList<PositionedPlayingFile> = ArrayList()

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			val library = Library()
			library.setId(1)
			val libraryProvider = PassThroughSpecificLibraryProvider(library)
			val libraryStorage = PassThroughLibraryStorage()
			val nowPlayingRepository = NowPlayingRepository(libraryProvider, libraryStorage)
			playbackEngine =
				PlaybackEngine(
					PreparedPlaybackQueueResourceManagement(
						fakePlaybackPreparerProvider
					) { 1 }, listOf(CompletingFileQueueProvider()),
					nowPlayingRepository,
					PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
				)

			playbackEngine
				?.setOnPlayingFileChanged { f -> positionedFiles.add(f) }
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
			fakePlaybackPreparerProvider.deferredResolution.resolve()
			playingPlaybackHandler.resolve()
			playbackEngine?.pause()
			nowPlaying =
				playbackEngine
					?.skipToNext()
					?.eventually { playbackEngine!!.skipToNext() }
					?.then { playbackEngine!!.resume() }
					?.then { fakePlaybackPreparerProvider.deferredResolution.resolve() }
					?.eventually { nowPlayingRepository.nowPlaying }
					?.toFuture()
					?.get()
		}
	}

	@Test
	fun thenThePlaybackStateIsPlaying() {
		assertThat(
			playbackEngine!!.isPlaying
		).isTrue
	}

	@Test
	fun thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(
			nowPlaying!!.playlistPosition
		).isEqualTo(3)
	}

	@Test
	fun thenTheSavedPlaylistIsCorrect() {
		assertThat(
			nowPlaying!!.playlist
		)
			.containsExactly(
				ServiceFile(1),
				ServiceFile(2),
				ServiceFile(3),
				ServiceFile(4),
				ServiceFile(5)
			)
	}

	@Test
	fun thenTheObservedFileIsCorrect() {
		assertThat(
			positionedFiles[positionedFiles.size - 1].playlistPosition
		).isEqualTo(3)
	}

	@Test
	fun thenTheFirstSkippedFileIsOnlyObservedOnce() {
		assertThat(
			positionedFiles
				.map { obj -> obj.asPositionedFile() })
			.containsOnlyOnce(PositionedFile(1, ServiceFile(2)))
	}

	@Test
	fun thenTheSecondSkippedFileIsNotObserved() {
		assertThat(
			positionedFiles
				.map { obj -> obj.asPositionedFile() })
			.doesNotContain(PositionedFile(2, ServiceFile(3)))
	}
}
