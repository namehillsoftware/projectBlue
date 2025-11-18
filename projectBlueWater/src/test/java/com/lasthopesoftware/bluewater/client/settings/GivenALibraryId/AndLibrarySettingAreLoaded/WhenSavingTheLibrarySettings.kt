package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
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

	private val savedLibrarySettings = mutableListOf<LibrarySettings>()

	private val services by lazy {
        LibrarySettingsViewModel(
			mockk {
				every { promiseLibrarySettings(libraryId) } returns LibrarySettings(
					libraryId = libraryId,
					isUsingExistingFiles = true,
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					connectionSettings = StoredSubsonicConnectionSettings(
						url = "b2q",
						isWakeOnLanEnabled = false,
						password = "hmpyA",
						macAddress = "S4YhepUHBcj",
					)
				).toPromise()
			},
			mockk {
				every { promiseSavedLibrarySettings(any()) } answers {
					val settings = firstArg<LibrarySettings>()
					savedLibrarySettings.add(settings)
					settings.toPromise()
				}
			},
			mockk(),
			mockk {
				every { promiseIsAllPermissionsGranted(any()) } returns true.toPromise()
			},
			mockk(),
			FakeStringResources(),
		)
    }

	private val connectionSettingsViewModel
		get() = services.connectionSettingsViewModel.value as? LibrarySettingsViewModel.SubsonicConnectionSettingsViewModel

	private var isSaved = false
	private var settingsChangedAfterSaving = false
	private var didSettingsChange = false
	private var didSettingsChangeAfterLoad = false
	private var didSettingsChangeAfterAccessCodeChanged = false
	private var didSettingsChangeAfterAccessCodeReverted = false

    @BeforeAll
    fun act() {
		with (services) {
			loadLibrary(libraryId).toExpiringFuture().get()

			(connectionSettingsViewModel.value as? LibrarySettingsViewModel.SubsonicConnectionSettingsViewModel)?.apply {
				didSettingsChangeAfterLoad = isSettingsChanged.value

				url.value = "V68Bp9rS"
				didSettingsChangeAfterAccessCodeChanged = isSettingsChanged.value

				url.value = "b2q"
				didSettingsChangeAfterAccessCodeReverted = isSettingsChanged.value

				url.value = "V68Bp9rS"
				password.value = "sl0Ha"
				userName.value = "xw9wy0T"
				libraryName.value = "left"
				isUsingExistingFiles.value = !isUsingExistingFiles.value
				isWakeOnLanEnabled.value = !isWakeOnLanEnabled.value
				syncedFileLocation.value = SyncedFileLocation.EXTERNAL
				macAddress.value = "sVU0zPNKdFu"

				didSettingsChange = isSettingsChanged.value
				isSaved = saveLibrary().toExpiringFuture().get() == true
				settingsChangedAfterSaving = isSettingsChanged.value
			}
		}
    }

	@Test
	fun `then the settings are not changed after load`() {
		assertThat(didSettingsChangeAfterLoad).isFalse
	}

	@Test
	fun `then the settings changed after the access code changed`() {
		assertThat(didSettingsChangeAfterAccessCodeChanged).isTrue
	}

	@Test
	fun `then the settings did not change after the access code changed`() {
		assertThat(didSettingsChangeAfterAccessCodeReverted).isFalse
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
        assertThat(connectionSettingsViewModel?.url?.value).isEqualTo("V68Bp9rS")
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
            .isEqualTo(SyncedFileLocation.EXTERNAL)
    }

    @Test
    fun `then is using existing files is correct`() {
        assertThat(services.isUsingExistingFiles.value).isFalse
    }

	@Test
	fun `then the library name is correct`() {
		assertThat(services.libraryName.value).isEqualTo("left")
	}

	@Test
	fun `then the mac address is correct`() {
		assertThat(connectionSettingsViewModel?.macAddress?.value).isEqualTo("sVU0zPNKdFu")
	}

	@Test
	fun `then the saved library settings are correct`() {
		assertThat(savedLibrarySettings).containsExactly(
			LibrarySettings(
				libraryId = libraryId,
				libraryName = "left",
				isUsingExistingFiles = false,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				StoredSubsonicConnectionSettings(
					url = "V68Bp9rS",
					isWakeOnLanEnabled = true,
					userName = "xw9wy0T",
					password = "sl0Ha",
					macAddress = "sVU0zPNKdFu",
					sslCertificateFingerprint = "",
				)
			)
		)
	}
}
