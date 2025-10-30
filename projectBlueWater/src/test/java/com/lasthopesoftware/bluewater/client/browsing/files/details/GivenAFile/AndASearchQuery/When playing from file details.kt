package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndASearchQuery

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.SearchedFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

// Needed for image bytes
class `When playing from file details` {

	companion object {
		private const val libraryId = 901
		private const val serviceFileId = "Pxp8s7MXI"
	}

	private var loadedLibraryId: LibraryId? = null
	private lateinit var startedLibraryId: LibraryId
	private lateinit var startedList: List<ServiceFile>
	private var startedPosition = -1

	private val mut by lazy {
		SearchedFileDetailsViewModel(
			mockk {
				every { startPlaylist(LibraryId(libraryId), any<List<ServiceFile>>(), any()) } answers {
					startedLibraryId = firstArg()
					startedList = secondArg()
					startedPosition = lastArg()
				}
			},
			mockk {
				every { load(any(), any()) } answers {
					loadedLibraryId = firstArg()
					Unit.toPromise()
				}
			},
			mockk {
				every { activeLibraryId } answers { loadedLibraryId }
			},
			mockk {
				every { promiseAudioFiles(LibraryId(libraryId), "nKqZO0RldS3") } returns listOf(
					ServiceFile("ooOjqFjkB"),
					ServiceFile("432.36"),
					ServiceFile("10005f0c-3208-4a1c-8ff2-4337f1c6b0f1"),
					ServiceFile(serviceFileId),
					ServiceFile("Cs9YycTDkx"),
					ServiceFile("ffZRcsT"),
				).toPromise()
			}
		)
	}

	@BeforeAll
	fun act() {
		mut.apply {
			load(
				LibraryId(libraryId),
				"nKqZO0RldS3",
				PositionedFile(0, ServiceFile(serviceFileId)),
			).toExpiringFuture().get()

			play()
		}
	}

	@Test
	fun `then the loaded library id is correct`() {
		assertThat(loadedLibraryId).isEqualTo(LibraryId(libraryId))
	}

	@Test
	fun `then the file is not playable with playlist`() {
		assertThat(mut.isPlayableWithPlaylist.value).isFalse
	}

	@Test
	fun `then the started library id is correct`() {
		assertThat(::startedLibraryId.isInitialized).isFalse
	}

	@Test
	fun `then the playlist is not started`() {
		assertThat(::startedList.isInitialized).isFalse
	}

	@Test
	fun `then the playlist is not started at any position`() {
		assertThat(startedPosition).isEqualTo(-1)
	}
}
