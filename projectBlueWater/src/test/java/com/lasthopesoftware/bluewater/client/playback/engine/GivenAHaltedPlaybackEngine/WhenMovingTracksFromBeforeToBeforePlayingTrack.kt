package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlaying
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenMovingTracksFromBeforeToBeforePlayingTrack {

	companion object {
		private const val libraryId = 1
	}

	private val mutt by lazy {
		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val storedLibrary =	Library(
			id = libraryId,
			savedTracksString = FileStringListUtilities.promiseSerializedFileStringList(
				listOf(
					ServiceFile("1"),
					ServiceFile("2"),
					ServiceFile("3"),
					ServiceFile("4"),
					ServiceFile("5")
				)
			).toExpiringFuture().get(),
			nowPlayingId = 3,
		)

		val libraryProvider = FakeLibraryRepository(storedLibrary)

		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val nowPlayingRepository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			libraryProvider,
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			nowPlayingRepository,
			listOf(CompletingFileQueueProvider()),
		)
		PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			playbackBootstrapper,
			playbackBootstrapper,
		)
	}

	private var updatedNowPlaying: NowPlaying? = null

	@BeforeAll
	fun act() {
		mutt.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture().get()
		updatedNowPlaying = mutt.moveFile(2, 0).toExpiringFuture()[1, TimeUnit.SECONDS]
	}

	@Test
	fun `then the playlist is updated`() {
		assertThat(updatedNowPlaying?.playlist).isEqualTo(
			listOf(
				ServiceFile("3"),
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("4"),
				ServiceFile("5")
			)
		)
	}

	@Test
	fun `then the playing file is correct`() {
		assertThat(updatedNowPlaying?.playlistPosition).isEqualTo(3)
	}
}
