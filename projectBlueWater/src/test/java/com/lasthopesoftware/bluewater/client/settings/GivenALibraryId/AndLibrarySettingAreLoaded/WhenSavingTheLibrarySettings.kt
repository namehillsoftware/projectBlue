package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
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

class WhenSavingTheLibrarySettings {

	private val libraryId = LibraryId(56)
    private val services by lazy {
		val libraryRepository = FakeLibraryRepository(
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
            mockk(),
			mockk {
				every { isReadPermissionsRequiredForLibrary(any()) } returns false
				every { isReadMediaPermissionsRequiredForLibrary(any()) } returns false
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

	private var isSaved = false

    @BeforeAll
    fun act() {
		with (services) {
			loadLibrary(libraryId).toExpiringFuture().get()
			accessCode.value = "V68Bp9rS"
			customSyncPath.value = "s39"
			password.value = "sl0Ha"
			userName.value = "xw9wy0T"
			libraryName.value = "left"
			isLocalOnly.value = !isLocalOnly.value
			isSyncLocalConnectionsOnly.value = !isSyncLocalConnectionsOnly.value
			isUsingExistingFiles.value = !isUsingExistingFiles.value
			isWakeOnLanEnabled.value = !isWakeOnLanEnabled.value
			syncedFileLocation.value = Library.SyncedFileLocation.CUSTOM
			isSaved = saveLibrary().toExpiringFuture().get() == true
		}
    }

	@Test
	fun `then the library is saved`() {
		assertThat(isSaved).isTrue
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(services.isStoragePermissionsNeeded.value).isFalse
	}

    @Test
    fun `then the access code is correct`() {
        assertThat(services.accessCode.value).isEqualTo("V68Bp9rS")
    }

    @Test
    fun `then the custom synced files path is correct`() {
        assertThat(services.customSyncPath.value).isEqualTo("s39")
    }

    @Test
    fun `then the connection is local only`() {
        assertThat(services.isLocalOnly.value).isTrue
    }

    @Test
    fun `then sync local only connections is correct`() {
        assertThat(services.isSyncLocalConnectionsOnly.value).isFalse
    }

    @Test
    fun `then wake on lan is correct`() {
        assertThat(services.isWakeOnLanEnabled.value).isTrue
    }

    @Test
    fun `then the user name is correct`() {
        assertThat(services.userName.value).isEqualTo("xw9wy0T")
    }

    @Test
    fun `then the password is correct`() {
        assertThat(services.password.value).isEqualTo("sl0Ha")
    }

    @Test
    fun `then synced file location is correct`() {
        assertThat(services.syncedFileLocation.value)
            .isEqualTo(Library.SyncedFileLocation.CUSTOM)
    }

    @Test
    fun `then is using existing files is correct`() {
        assertThat(services.isUsingExistingFiles.value).isFalse
    }

	@Test
	fun `then the library name is correct`() {
		assertThat(services.libraryName.value).isEqualTo("left")
	}
}
