package com.lasthopesoftware.bluewater.client.settings.GivenANewLibrary.AndSettingsAreChanged

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSavingTheLibrarySettings {

	private val services by lazy {
		val libraryRepository = FakeLibraryRepository()

        LibrarySettingsViewModel(
            libraryRepository,
            libraryRepository,
            mockk(),
			mockk {
				every { promiseIsLibraryPermissionsGranted(any()) } returns true.toPromise()
			},
		)
    }
	private var isSaved = false
	private var settingsChangedAfterSaving = false
	private var didSettingsChange = false
	private var initialSettingsChanged = false
	private var didSettingsChangeAfterAccessCodeChanged = false

    @BeforeAll
    fun act() {
		with (services) {
			initialSettingsChanged = isSettingsChanged.value

			userName.value = "xw9wy0T"
			didSettingsChangeAfterAccessCodeChanged = isSettingsChanged.value

			accessCode.value = "7P9tU"
			password.value = "sl0Ha"
			libraryName.value = "left"
			isLocalOnly.value = !isLocalOnly.value
			isSyncLocalConnectionsOnly.value = !isSyncLocalConnectionsOnly.value
			isUsingExistingFiles.value = !isUsingExistingFiles.value
			isWakeOnLanEnabled.value = !isWakeOnLanEnabled.value
			syncedFileLocation.value = Library.SyncedFileLocation.INTERNAL

			didSettingsChange = isSettingsChanged.value
			isSaved = saveLibrary().toExpiringFuture().get() == true
			settingsChangedAfterSaving = isSettingsChanged.value
		}
    }

	@Test
	fun `then the settings are not changed after load`() {
		assertThat(initialSettingsChanged).isFalse
	}

	@Test
	fun `then the settings changed after the access code changed`() {
		assertThat(didSettingsChangeAfterAccessCodeChanged).isTrue
	}

	@Test
	fun `then the settings changed`() {
		assertThat(didSettingsChange).isTrue
	}

	@Test
	fun `then the library is saved`() {
		assertThat(isSaved).isTrue
	}

	@Test
	fun `then the settings are changed after saving`() {
		assertThat(settingsChangedAfterSaving).isFalse
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(services.isStoragePermissionsNeeded.value).isFalse
	}

    @Test
    fun `then the access code is correct`() {
        assertThat(services.accessCode.value).isEqualTo("7P9tU")
    }

    @Test
    fun `then the connection is local only`() {
        assertThat(services.isLocalOnly.value).isTrue
    }

    @Test
    fun `then sync local only connections is correct`() {
        assertThat(services.isSyncLocalConnectionsOnly.value).isTrue
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
            .isEqualTo(Library.SyncedFileLocation.INTERNAL)
    }

    @Test
    fun `then is using existing files is correct`() {
        assertThat(services.isUsingExistingFiles.value).isTrue
    }

	@Test
	fun `then the library name is correct`() {
		assertThat(services.libraryName.value).isEqualTo("left")
	}
}
