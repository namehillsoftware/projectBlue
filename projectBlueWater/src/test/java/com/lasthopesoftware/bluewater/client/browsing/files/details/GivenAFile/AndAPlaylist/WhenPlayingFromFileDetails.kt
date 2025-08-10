package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndAPlaylist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.ListedFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

// Needed for image bytes
class WhenPlayingFromFileDetails {

	companion object {
		private const val libraryId = 591
		private const val serviceFileId = "338"
	}

	private var loadedLibraryId: LibraryId? = null
	private lateinit var startedLibraryId: LibraryId
	private lateinit var startedList: List<ServiceFile>
	private var startedPosition = -1

	private val mut by lazy {
		ListedFileDetailsViewModel(
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
		)
	}

	@BeforeAll
	fun act() {
		mut.apply {
			load(
				LibraryId(libraryId),
				listOf(
					ServiceFile("830"),
					ServiceFile(serviceFileId),
					ServiceFile("628"),
					ServiceFile("537"),
					ServiceFile("284"),
					ServiceFile("419"),
					ServiceFile("36"),
					ServiceFile("396"),
				),
				1,
			).toExpiringFuture().get()

			play()
		}
	}

	@Test
	fun `then the loaded library id is correct`() {
		assertThat(loadedLibraryId).isEqualTo(LibraryId(libraryId))
	}

	@Test
	fun `then the started library id is correct`() {
		assertThat(startedLibraryId).isEqualTo(LibraryId(libraryId))
	}

	@Test
	fun `then the correct playlist is started`() {
		assertThat(startedList).containsExactlyInAnyOrder(
			ServiceFile("830"),
			ServiceFile(serviceFileId),
			ServiceFile("628"),
			ServiceFile("537"),
			ServiceFile("284"),
			ServiceFile("419"),
			ServiceFile("36"),
			ServiceFile("396"),
		)
	}

	@Test
	fun `then the playlist is started at the correct position`() {
		assertThat(startedPosition).isEqualTo(1)
	}
}
