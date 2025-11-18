package com.lasthopesoftware.bluewater.client.settings.GivenANewLibrary.AndSettingsAreChanged

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSavingTheLibrarySettings {

	companion object {
		private const val newLibraryId = 918
	}

	private val services by lazy {
		LibrarySettingsViewModel(
            mockk(),
			mockk {
				every { promiseSavedLibrarySettings(any()) } answers {
					firstArg<LibrarySettings>().copy(libraryId = LibraryId(newLibraryId)).toPromise()
				}
			},
			mockk(),
			mockk {
				every { promiseIsAllPermissionsGranted(any()) } returns true.toPromise()
			},
			mockk {
				every { promiseIsConnectionActive(any()) } returns false.toPromise()
			},
			FakeStringResources(),
		)
    }

	private val mediaCenterConnectionSettingsViewModel
		get() = services.connectionSettingsViewModel.value as? LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel

	private var isSaved = false
	private var settingsChangedAfterSaving = false
	private var didSettingsChange = false
	private var initialSettingsChanged = false
	private var didSettingsChangeAfterConnectionSettingsChosen = false

    @BeforeAll
    fun act() {
		with (services) {
			initialSettingsChanged = isSettingsChanged.value

			connectionSettingsViewModel.value = availableConnectionSettings.first { it is LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel }
			didSettingsChangeAfterConnectionSettingsChosen = isSettingsChanged.value

			mediaCenterConnectionSettingsViewModel?.apply {
				userName.value = "xw9wy0T"

				accessCode.value = "7P9tU"
				password.value = "sl0Ha"
				libraryName.value = "left"
				isLocalOnly.value = !isLocalOnly.value
				isSyncLocalConnectionsOnly.value = !isSyncLocalConnectionsOnly.value
				isUsingExistingFiles.value = !isUsingExistingFiles.value
				isWakeOnLanEnabled.value = !isWakeOnLanEnabled.value
				syncedFileLocation.value = SyncedFileLocation.INTERNAL

				didSettingsChange = isSettingsChanged.value
				isSaved = saveLibrary().toExpiringFuture().get() == true
				settingsChangedAfterSaving = isSettingsChanged.value
			}
		}
    }

	@Test
	fun `then the library id is updated`() {
		assertThat(services.activeLibraryId).isEqualTo(LibraryId(918))
	}

	@Test
	fun `then the settings are not changed after load`() {
		assertThat(initialSettingsChanged).isFalse
	}

	@Test
	fun `then the settings changed after the connection settings were chosen`() {
		assertThat(didSettingsChangeAfterConnectionSettingsChosen).isTrue
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
	fun `then the settings are not changed after saving`() {
		assertThat(settingsChangedAfterSaving).isFalse
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(services.isStoragePermissionsNeeded.value).isFalse
	}

    @Test
    fun `then the access code is correct`() {
        assertThat(mediaCenterConnectionSettingsViewModel?.accessCode?.value).isEqualTo("7P9tU")
    }

    @Test
    fun `then the connection is local only`() {
        assertThat(mediaCenterConnectionSettingsViewModel?.isLocalOnly?.value).isTrue
    }

    @Test
    fun `then sync local only connections is correct`() {
        assertThat(mediaCenterConnectionSettingsViewModel?.isSyncLocalConnectionsOnly?.value).isTrue
    }

    @Test
    fun `then wake on lan is correct`() {
        assertThat(mediaCenterConnectionSettingsViewModel?.isWakeOnLanEnabled?.value).isTrue
    }

    @Test
    fun `then the user name is correct`() {
        assertThat(mediaCenterConnectionSettingsViewModel?.userName?.value).isEqualTo("xw9wy0T")
    }

    @Test
    fun `then the password is correct`() {
        assertThat(mediaCenterConnectionSettingsViewModel?.password?.value).isEqualTo("sl0Ha")
    }

    @Test
    fun `then synced file location is correct`() {
        assertThat(services.syncedFileLocation.value)
            .isEqualTo(SyncedFileLocation.INTERNAL)
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
