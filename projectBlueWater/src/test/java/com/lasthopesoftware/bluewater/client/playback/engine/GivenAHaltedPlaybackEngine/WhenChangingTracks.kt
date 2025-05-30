package com.lasthopesoftware.bluewater.client.playback.engine.GivenAHaltedPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.ManagedPlaylistPlayer
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenChangingTracks {

	companion object {
		private const val libraryId = 311
	}

	private val mut by lazy {

		val fakePlaybackPreparerProvider = FakeDeferredPlayableFilePreparationSourceProvider()
		val library = Library(
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
			nowPlayingId = 0,
			nowPlayingProgress = 388708L,
		)

		val libraryProvider = FakeLibraryRepository(library)
		val savedLibrary = object : Promise<Library>() {
			val libraryStorage = spyk(libraryProvider) {
				every { updateNowPlaying(any()) } answers {
					val values = firstArg<LibraryNowPlayingValues>()
					libraryProvider.updateNowPlaying(values).then { _ ->
						resolve(libraryProvider.libraries[values.id])
					}
				}
			}
		}
		val preparedPlaybackQueueResourceManagement = PreparedPlaybackQueueResourceManagement(
			fakePlaybackPreparerProvider,
			mockk {
				every { maxQueueSize } returns 1
			}
		)
		val nowPlayingRepository = NowPlayingRepository(
			FakeSelectedLibraryProvider(),
			savedLibrary.libraryStorage,
		)
		val playbackBootstrapper = ManagedPlaylistPlayer(
			PlaylistVolumeManager(1.0f),
			preparedPlaybackQueueResourceManagement,
			nowPlayingRepository,
			listOf(CompletingFileQueueProvider()),
		)
		val playbackEngine = PlaybackEngine(
			preparedPlaybackQueueResourceManagement,
			listOf(CompletingFileQueueProvider()),
			nowPlayingRepository,
			playbackBootstrapper,
			playbackBootstrapper,
		)

		Pair(savedLibrary, playbackEngine)
	}
	private val library: Library?
		get() = mut.first.toExpiringFuture().get()
	private var initialState: PositionedProgressedFile? = null
	private var nextSwitchedFile: PositionedFile? = null

	@BeforeAll
	fun before() {
		val (_, playbackEngine) = mut
		initialState = playbackEngine.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture().get()?.second
		nextSwitchedFile = playbackEngine.changePosition(3, Duration.ZERO).toExpiringFuture()[1, TimeUnit.SECONDS]?.second
	}

	@Test
	fun `then the initial playlist position is correct`() {
		assertThat(initialState?.playlistPosition).isEqualTo(0)
	}

	@Test
	fun `then the next file change is the correct playlist position`() {
		assertThat(nextSwitchedFile!!.playlistPosition).isEqualTo(3)
	}

	@Test
	fun `then the saved library is at the correct playlist position`() {
		assertThat(library?.nowPlayingId).isEqualTo(3)
	}

	@Test
	fun `then the saved library is at the correct file position`() {
		assertThat(library?.nowPlayingProgress).isEqualTo(0)
	}
}
