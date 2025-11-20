package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded.AndNotAllPermissionsAreGranted

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
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

	private val libraryId = LibraryId(56)
    private val services by lazy {
		LibrarySettingsViewModel(
            mockk {
				every { promiseLibrarySettings(libraryId) } returns LibrarySettings(
					libraryId = libraryId,
					isUsingExistingFiles = true,
					libraryName = "theater",
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					connectionSettings = StoredMediaCenterConnectionSettings(
						accessCode = "b2q",
						isLocalOnly = false,
						isSyncLocalConnectionsOnly = true,
						isWakeOnLanEnabled = false,
						password = "hmpyA",
					)
				).toPromise()
			},
			mockk(),
			mockk(),
			mockk {
				every { promiseIsAllPermissionsGranted(any()) } returns false.toPromise()
			},
			mockk {
				every { promiseIsConnectionActive(libraryId) } returns false.toPromise()
			},
			FakeStringResources(),
		)
    }

	private val connectionSettingsViewModel
		get() = services.connectionSettingsViewModel.value as? LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel

	private var isSaved = false

    @BeforeAll
    fun act() {
		with (services) {
			services.loadLibrary(libraryId).toExpiringFuture().get()
			(connectionSettingsViewModel.value as? LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel)?.apply {
				accessCode.value = "V68Bp9rS"
				password.value = "sl0Ha"
				userName.value = "xw9wy0T"
				isLocalOnly.value = !isLocalOnly.value
				isSyncLocalConnectionsOnly.value = !isSyncLocalConnectionsOnly.value
				isUsingExistingFiles.value = !isUsingExistingFiles.value
				isWakeOnLanEnabled.value = !isWakeOnLanEnabled.value
				syncedFileLocation.value = SyncedFileLocation.INTERNAL
				libraryName.value = "spit"
				isSaved = saveLibrary().toExpiringFuture().get() == true
			}
		}
    }

	@Test
	fun `then the library is not saved`() {
		assertThat(isSaved).isFalse
	}

	@Test
	fun `then permissions are required`() {
		assertThat(services.isStoragePermissionsNeeded.value).isTrue
	}

    @Test
    fun `then the access code is correct`() {
        assertThat(connectionSettingsViewModel?.accessCode?.value).isEqualTo("V68Bp9rS")
    }

    @Test
    fun `then the connection is local only`() {
        assertThat(connectionSettingsViewModel?.isLocalOnly?.value).isTrue
    }

    @Test
    fun `then sync local only connections is correct`() {
        assertThat(connectionSettingsViewModel?.isSyncLocalConnectionsOnly?.value).isFalse
    }

    @Test
    fun `then wake on lan is correct`() {
        assertThat(connectionSettingsViewModel?.isWakeOnLanEnabled?.value).isTrue
    }

    @Test
    fun `then the user name is correct`() {
        assertThat(connectionSettingsViewModel?.userName?.value).isEqualTo("xw9wy0T")
    }

    @Test
    fun `then the password is correct`() {
        assertThat(connectionSettingsViewModel?.password?.value).isEqualTo("sl0Ha")
    }

    @Test
    fun `then synced file location is correct`() {
        assertThat(services.syncedFileLocation.value)
            .isEqualTo(SyncedFileLocation.INTERNAL)
    }

    @Test
    fun `then is using existing files is correct`() {
        assertThat(services.isUsingExistingFiles.value).isFalse
    }

	@Test
	fun `then the library name is correct`() {
		assertThat(services.libraryName.value).isEqualTo("spit")
	}
}
