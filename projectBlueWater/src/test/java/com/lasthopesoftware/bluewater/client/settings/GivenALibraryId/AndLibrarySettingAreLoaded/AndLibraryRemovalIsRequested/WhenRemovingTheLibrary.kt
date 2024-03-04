package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded.AndLibraryRemovalIsRequested

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRemovingTheLibrary {

	private val libraryId = LibraryId(208)
    private val services by lazy {
		val libraryRepository = FakeLibraryRepository(
			Library(
				id = libraryId.id,
				accessCode = "b2q",
				isLocalOnly = false,
				isSyncLocalConnectionsOnly = true,
				isWakeOnLanEnabled = false,
				password = "hmpyA",
				syncedFileLocation = Library.SyncedFileLocation.EXTERNAL,
				isUsingExistingFiles = true,
			)
		)

        LibrarySettingsViewModel(
            libraryRepository,
            libraryRepository,
            mockk {
				every { removeLibrary(any()) } answers {
					removedLibraries.add(firstArg())
					Unit.toPromise()
				}
			},
			mockk(),
		)
    }

	private val removedLibraries = mutableListOf<Library>()

    @BeforeAll
    fun act() {
		with (services) {
			loadLibrary(libraryId).toExpiringFuture().get()
			requestLibraryRemoval()
			removeLibrary().toExpiringFuture().get()
		}
    }

	@Test
	fun `then the library is removed`() {
		assertThat(removedLibraries.single().libraryId).isEqualTo(libraryId)
	}
}
