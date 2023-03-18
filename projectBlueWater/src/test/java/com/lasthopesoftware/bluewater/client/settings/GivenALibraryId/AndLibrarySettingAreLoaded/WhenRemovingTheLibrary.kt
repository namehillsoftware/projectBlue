package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenRemovingTheLibrary {

	private val libraryId = LibraryId(354)
    private val services by lazy {
		val libraryRepository = FakeLibraryProvider(
			Library(
				_id = libraryId.id,
				_accessCode = "b2q",
				_customSyncedFilesPath = "d6DL91i",
				_isLocalOnly = false,
				_isSyncLocalConnectionsOnly = true,
				_isWakeOnLanEnabled = false,
				_userName = "o0PoFzNL",
				_password = "hmpyA",
				_syncedFileLocation = Library.SyncedFileLocation.EXTERNAL,
				_isUsingExistingFiles = true,
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
			mockk {
				every { isReadPermissionsRequiredForLibrary(any()) } returns false
			},
			mockk {
				every { isWritePermissionsRequiredForLibrary(any()) } returns false
			},
			mockk {
				every { requestPermissions(any()) } answers {
					firstArg<List<String>>().associateWith { true }.toPromise()
				}
			},
        )
    }

	private val removedLibraries = mutableListOf<Library>()

    @BeforeAll
    fun act() {
		with (services) {
			loadLibrary(libraryId).toExpiringFuture().get()
			removeLibrary().toExpiringFuture().get()
		}
    }

	@Test
	fun `then the library is not removed`() {
		assertThat(removedLibraries).isEmpty()
	}
}
