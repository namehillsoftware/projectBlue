package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryNowPlayingValues
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
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
import java.util.concurrent.TimeUnit

class WhenRemovingTheCurrentlyPlayingFile {

	companion object {
		private const val libraryId = 837
		private const val playingPosition = 5
	}

	private val playlist = listOf(
		ServiceFile("1"),
		ServiceFile("2"),
		ServiceFile("3"),
		ServiceFile("4"),
		ServiceFile("5"),
		ServiceFile("13"),
		ServiceFile("27")
	)

	private val expectedPlaylist = listOf(
		ServiceFile("1"),
		ServiceFile("2"),
		ServiceFile("3"),
		ServiceFile("4"),
		ServiceFile("5"),
		ServiceFile("27")
	)

	private val mut by lazy {
		val fakePlaybackPreparerProvider = FakeMappedPlayableFilePreparationSourceProvider(playlist)
		val startingSavedTracksString = FileStringListUtilities.promiseSerializedFileStringList(playlist).toExpiringFuture().get()
		val library = Library(
			id = libraryId,
			savedTracksString = startingSavedTracksString,
			nowPlayingId = playingPosition,
		)

		val libraryProvider = FakeLibraryRepository(library)
		val savedLibrary = object : Promise<Library>() {
			val libraryStorage = spyk(libraryProvider) {
				every { updateNowPlaying(any()) } answers {
					val values = firstArg<LibraryNowPlayingValues>()
					libraryProvider.updateNowPlaying(values).then { _ ->
						val lib = libraryProvider.libraries[libraryId]
						if (lib?.savedTracksString != library.savedTracksString && lib?.nowPlayingId == 5)
							resolve(lib)
					}
				}
			}
		}

		val playbackEngine =
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
				listOf(CompletingFileQueueProvider()),
				NowPlayingRepository(
					FakeSelectedLibraryProvider(),
					savedLibrary.libraryStorage,
				),
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
		Triple(fakePlaybackPreparerProvider, savedLibrary, playbackEngine)
	}

	private val savedLibrary: Library?
		get() = mut.second.toExpiringFuture().get()

	private var initialState: PositionedProgressedFile? = null
	private var positionedPlayingFile: PositionedPlayingFile? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, _, playbackEngine) = mut

		initialState = playbackEngine.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture().get()?.second
		playbackEngine.resume().toExpiringFuture()[1, TimeUnit.SECONDS]

		fakePlaybackPreparerProvider.preparationSourceBeingProvided { _, deferredPreparedPlayableFile ->
			deferredPreparedPlayableFile.resolve()
		}

		val promisedPlayingFile = object : Promise<PositionedPlayingFile>() {
			init {
				playbackEngine.setOnPlayingFileChanged { _, c ->
					resolve(c)
				}
			}
		}

		val futurePlaying = playbackEngine.removeFileAtPosition(playingPosition).toExpiringFuture()
		futurePlaying[1, TimeUnit.SECONDS]
		positionedPlayingFile = promisedPlayingFile.toExpiringFuture().get()
	}

	@Test
	fun `then the currently playing file position is the same`() {
		assertThat(savedLibrary?.nowPlayingId).isEqualTo(5)
	}

	@Test
	fun `then the file is removed`() {
		assertThat(savedLibrary?.savedTracksString).isEqualTo(
			FileStringListUtilities.promiseSerializedFileStringList(expectedPlaylist).toExpiringFuture().get()
		)
	}

	@Test
	fun `then the file queue is shifted to the next file`() {
		assertThat(positionedPlayingFile!!.serviceFile).isEqualTo(ServiceFile("27"))
	}

	@Test
	fun `then the initial service file is correct`() {
		assertThat(initialState?.serviceFile).isEqualTo(ServiceFile("13"))
	}
}
