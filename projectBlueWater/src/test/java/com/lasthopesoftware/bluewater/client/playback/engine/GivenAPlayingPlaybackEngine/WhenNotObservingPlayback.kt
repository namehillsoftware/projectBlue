package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeMappedPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenNotObservingPlayback {

	private val libraryProvider by lazy {
		val library = Library(id = 1, nowPlayingId = 5)

		FakeLibraryRepository(library)
	}

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(
			listOf(
				ServiceFile("1"),
				ServiceFile("2"),
				ServiceFile("3"),
				ServiceFile("4"),
				ServiceFile("5")
			)
		)

		val promisedLibraryStorageUpdate = object : Promise<Unit>() {
			val storage = spyk(libraryProvider) {
				every { updateNowPlaying(any()) } answers {
					val values = firstArg<LibraryNowPlayingValues>()
					libraryProvider.updateNowPlaying(values).then { _ ->
						if (libraryProvider.libraries[values.id]?.nowPlayingId == 1)
							resolve(Unit)
					}
				}
			}
		}

		val preparedPlaybackQueueResourceManagement =
			PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration())
		val repository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			promisedLibraryStorageUpdate.storage
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			repository,
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine = PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider()),
			repository,
			playbackBootstrapper,
			playbackBootstrapper,
		)

		Triple(fakePlaybackPreparerProvider, promisedLibraryStorageUpdate, playbackEngine)
	}

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, promisedLibraryStorageUpdate, playbackEngine) = mut
		val library = libraryProvider.libraries[1]!!
		playbackEngine
			.startPlaylist(
				library.libraryId,
				fakePlaybackPreparerProvider.deferredResolutions.keys.toList(),
				0
			)
			.toExpiringFuture()
			.get()
		val resolvablePlaybackHandler = fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("1")]?.resolve()
		fakePlaybackPreparerProvider.deferredResolutions[ServiceFile("2")]?.resolve()
		resolvablePlaybackHandler?.resolve()
		promisedLibraryStorageUpdate.toExpiringFuture().get()
	}

	@Test
	fun `then the saved track position is one`() {
		assertThat(libraryProvider.libraries[1]!!.nowPlayingId).isEqualTo(1)
	}

	@Test
	fun `then the manager is playing`() {
		assertThat(mut.third.isPlaying).isTrue
	}
}
