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
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenPlaybackCompletes {

	companion object {
		private var playbackEngine: PlaybackEngine? = null
		private var nowPlaying: NowPlaying? = null
		private var observedPlayingFile: PositionedPlayingFile? = null
		private var resetPositionedFile: PositionedFile? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
			val library = Library()
			library.setId(1)
			val libraryProvider = PassThroughSpecificLibraryProvider(library)
			val libraryStorage = PassThroughLibraryStorage()
			val nowPlayingRepository = NowPlayingRepository(libraryProvider, libraryStorage)
			playbackEngine = PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(
					fakePlaybackPreparerProvider
				) { 1 }, listOf(CompletingFileQueueProvider()),
				nowPlayingRepository,
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
			playbackEngine
				?.setOnPlayingFileChanged { f -> observedPlayingFile = f }
				?.setOnPlaylistReset { f -> resetPositionedFile = f }
				?.startPlaylist(
					listOf(
						ServiceFile(1),
						ServiceFile(2),
						ServiceFile(3),
						ServiceFile(4),
						ServiceFile(5)
					), 0, Duration.ZERO
				)
			var playingPlaybackHandler = fakePlaybackPreparerProvider.deferredResolution.resolve()
			for (i in 0..3) {
				val newPlayingPlaybackHandler =
					fakePlaybackPreparerProvider.deferredResolution.resolve()
				playingPlaybackHandler.resolve()
				playingPlaybackHandler = newPlayingPlaybackHandler
			}
			playingPlaybackHandler.resolve()
			nowPlaying = FuturePromise(nowPlayingRepository.nowPlaying).get()
		}
	}

	@Test
	fun thenThePlaybackStateIsNotPlaying() {
		assertThat(playbackEngine!!.isPlaying).isFalse
	}

	@Test
	fun thenTheObservedFilePositionIsCorrect() {
		assertThat(observedPlayingFile!!.asPositionedFile())
			.isEqualTo(PositionedFile(4, ServiceFile(5)))
	}

	@Test
	fun thenTheResetFilePositionIsZero() {
		assertThat(resetPositionedFile).isEqualTo(PositionedFile(0, ServiceFile(1)))
	}

	@Test
	fun thenTheSavedFilePositionIsCorrect() {
		assertThat(nowPlaying!!.filePosition).isEqualTo(0)
	}

	@Test
	fun thenTheSavedPlaylistPositionIsCorrect() {
		assertThat(nowPlaying!!.playlistPosition).isEqualTo(0)
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
