package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CyclicalFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenSettingEngineToRepeat {
	private val nowPlaying by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library(
			id = 907,
			savedTracksString = FileStringListUtilities.promiseSerializedFileStringList(
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				)
			).toExpiringFuture().get(),
			nowPlayingId = 0,
		)
		val libraryProvider = FakeLibraryRepository(library)

		val repository =
			NowPlayingRepository(
				FakeSelectedLibraryProvider(),
				libraryProvider,
			)
		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			repository,
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
		)
		val playbackEngine = PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider(), CyclicalFileQueueProvider()),
			repository,
			playbackBootstrapper,
			playbackBootstrapper,
		)
		playbackEngine.restoreFromSavedState(library.libraryId).toExpiringFuture().get()
		playbackEngine.playRepeatedly().toExpiringFuture().get()
		repository.promiseNowPlaying(library.libraryId).toExpiringFuture().get()
	}

	@Test
	fun `then now playing is set to repeating`() {
		assertThat(nowPlaying?.isRepeating).isTrue
	}
}
