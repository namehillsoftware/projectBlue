package com.lasthopesoftware.bluewater.client.playback.engine.GivenAPlayingPlaybackEngine

import com.lasthopesoftware.TestUrl
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.FileStringListUtilities
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.IFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakePlaybackQueueConfiguration
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.selected.GivenANullConnection.AndTheSelectedLibraryChanges.FakeSelectedLibraryProvider
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.engine.PlaybackEngine
import com.lasthopesoftware.bluewater.client.playback.engine.bootstrap.PlaylistPlaybackBootstrapper
import com.lasthopesoftware.bluewater.client.playback.engine.preparation.PreparedPlaybackQueueResourceManagement
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile
import com.lasthopesoftware.bluewater.client.playback.file.PositionedProgressedFile
import com.lasthopesoftware.bluewater.client.playback.file.preparation.FakeDeferredPlayableFilePreparationSourceProvider
import com.lasthopesoftware.bluewater.client.playback.file.preparation.queues.CompletingFileQueueProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.FakeNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.NowPlayingRepository
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenRemovingTheCurrentlyPlayingFile {

	companion object {
		private const val libraryId = 837
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
					ServiceFile("5"),
					ServiceFile("13"),
					ServiceFile("27")
				)
			).toExpiringFuture().get(),
			nowPlayingId = 5,
		)

		val libraryProvider = FakeLibraryRepository(library)
		val savedLibrary = object : Promise<Library>() {
			val libraryStorage = mockk<ILibraryStorage> {
				every { updateNowPlaying(any(), any(), any(), any(), any()) } answers {
					libraryProvider.updateNowPlaying(arg(0), arg(1), arg(2), arg(3), arg(4)).then { _ ->
						val lib = libraryProvider.libraries[libraryId]
						if (lib?.savedTracksString != library.savedTracksString && lib?.nowPlayingId == 5)
							resolve(lib)
					}
				}
			}
		}

		val filePropertiesContainerRepository = mockk<IFilePropertiesContainerRepository>()
		every {
			filePropertiesContainerRepository.getFilePropertiesContainer(UrlKeyHolder(TestUrl, ServiceFile("5")))
		} returns FilePropertiesContainer(1, mapOf(Pair(KnownFileProperties.Duration, "100")))

		val playbackEngine =
			PlaybackEngine(
				PreparedPlaybackQueueResourceManagement(fakePlaybackPreparerProvider, FakePlaybackQueueConfiguration()),
				listOf(CompletingFileQueueProvider()),
				NowPlayingRepository(
					FakeSelectedLibraryProvider(),
					libraryProvider,
					savedLibrary.libraryStorage,
					FakeNowPlayingState(),
				),
				PlaylistPlaybackBootstrapper(PlaylistVolumeManager(1.0f))
			)
		Triple(fakePlaybackPreparerProvider, savedLibrary, playbackEngine)
	}

	private var initialState: PositionedProgressedFile? = null
	private var savedLibrary: Library? = null
	private var positionedPlayingFile: PositionedPlayingFile? = null

	@BeforeAll
	fun act() {
		val (fakePlaybackPreparerProvider, promisedSave, playbackEngine) = mut

		initialState = playbackEngine.restoreFromSavedState(LibraryId(libraryId)).toExpiringFuture().get()?.second
		playbackEngine.resume().toExpiringFuture()[1, TimeUnit.SECONDS]
		fakePlaybackPreparerProvider.deferredResolution.resolve()
		playbackEngine.setOnPlayingFileChanged { _, c -> positionedPlayingFile = c	}
		val futurePlaying = playbackEngine.removeFileAtPosition(5).toExpiringFuture()
		fakePlaybackPreparerProvider.deferredResolution.resolve()
		futurePlaying[1, TimeUnit.SECONDS]
		savedLibrary = promisedSave.toExpiringFuture().get()
	}

	@Test
	fun `then the currently playing file position is the same`() {
		assertThat(savedLibrary?.nowPlayingId).isEqualTo(5)
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
