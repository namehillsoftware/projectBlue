package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When saving and testing the library settings` {

	private val libraryId = LibraryId(56)

	private val services by lazy {
		val deferredTestedConnection = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Pair(
			deferredTestedConnection,
			LibrarySettingsViewModel(
				mockk {
					every { promiseLibrarySettings(libraryId) } returns LibrarySettings(
						libraryId = libraryId,
						isUsingExistingFiles = true,
						syncedFileLocation = SyncedFileLocation.EXTERNAL,
						connectionSettings = StoredMediaCenterConnectionSettings(
							accessCode = "b2q",
							isLocalOnly = false,
							isSyncLocalConnectionsOnly = true,
							isWakeOnLanEnabled = false,
							password = "hmpyA",
							macAddress = "S4YhepUHBcj",
						)
					).toPromise()
				},
				mockk {
					every { promiseSavedLibrarySettings(any()) } answers {
						val savedSettings = firstArg<LibrarySettings>()
						savedLibrarySettings = savedSettings
						savedSettings.toPromise()
					}
				},
				mockk(),
				mockk {
					every { promiseIsAllPermissionsGranted(any()) } returns true.toPromise()
				},
				mockk {
					every { promiseTestedLibraryConnection(libraryId) } returns deferredTestedConnection
					every { promiseIsConnectionActive(libraryId) } returns false.toPromise()
				},
				FakeStringResources(
					connecting = "1iOiFTM6lg",
					gettingLibrary = "9XUwu816PYN",
					connectingToServerLibrary = "o2C3Emsri",
					connected = "TB3IyvtaWr"
				),
			)
		)
    }

	private val connectionSettingsViewModel: LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel?
		get() = services.second.connectionSettingsViewModel.value as? LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel

	private val connectionStatuses = mutableListOf<String>()

	private val isTestingConnectionStates = mutableListOf<Boolean>()
	private var savedLibrarySettings: LibrarySettings? = null
	private var settingsChangedAfterSaving = false
	private var didSettingsChange = false
	private var didSettingsChangeAfterLoad = false
	private var didSettingsChangeAfterAccessCodeChanged = false
	private var didSettingsChangeAfterAccessCodeReverted = false

    @BeforeAll
    fun act() {
		val (promise, viewModel) = services
		with (viewModel) {
			connectionStatus.mapNotNull().subscribe(connectionStatuses::add).toCloseable().use {
				isTestingConnection.mapNotNull().subscribe(isTestingConnectionStates::add).toCloseable().use {
					loadLibrary(libraryId).toExpiringFuture().get()
					didSettingsChangeAfterLoad = isSettingsChanged.value

					(this@`When saving and testing the library settings`.connectionSettingsViewModel)?.apply {
						accessCode.value = "V68Bp9rS"
						didSettingsChangeAfterAccessCodeChanged = isSettingsChanged.value

						accessCode.value = "b2q"
						didSettingsChangeAfterAccessCodeReverted = isSettingsChanged.value

						accessCode.value = "V68Bp9rS"
						password.value = "sl0Ha"
						userName.value = "xw9wy0T"
						libraryName.value = "left"
						isLocalOnly.value = !isLocalOnly.value
						isSyncLocalConnectionsOnly.value = !isSyncLocalConnectionsOnly.value
						isUsingExistingFiles.value = !isUsingExistingFiles.value
						isWakeOnLanEnabled.value = !isWakeOnLanEnabled.value
						syncedFileLocation.value = SyncedFileLocation.EXTERNAL
						macAddress.value = "sVU0zPNKdFu"

						didSettingsChange = isSettingsChanged.value

						val promisedSaveAndTest = saveAndTestLibrary()

						promise.sendProgressUpdates(
							BuildingConnectionStatus.GettingLibrary,
							BuildingConnectionStatus.BuildingConnection,
							BuildingConnectionStatus.BuildingConnectionComplete
						)
						promise.sendResolution(mockk())

						promisedSaveAndTest.toExpiringFuture().get()
						settingsChangedAfterSaving = isSettingsChanged.value
					}
				}
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
	fun `then the connection is possible`() {
		assertThat(services.second.isConnectionPossible.value).isTrue
	}

	@Test
	fun `then the settings are changed after saving`() {
		assertThat(settingsChangedAfterSaving).isFalse
	}

	@Test
	fun `then permissions are not required`() {
		assertThat(services.second.isStoragePermissionsNeeded.value).isFalse
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
        assertThat(services.second.syncedFileLocation.value)
            .isEqualTo(SyncedFileLocation.EXTERNAL)
    }

    @Test
    fun `then is using existing files is correct`() {
        assertThat(services.second.isUsingExistingFiles.value).isFalse
    }

	@Test
	fun `then the library name is correct`() {
		assertThat(services.second.libraryName.value).isEqualTo("left")
	}

	@Test
	fun `then the mac address is correct`() {
		assertThat(connectionSettingsViewModel?.macAddress?.value).isEqualTo("sVU0zPNKdFu")
	}

	@Test
	fun `then the connection statuses are correct`() {
		assertThat(connectionStatuses).containsExactly(
			"",
			"9XUwu816PYN",
			"o2C3Emsri",
			"TB3IyvtaWr",
		)
	}

	@Test
	fun `then the is testing connection states are correct`() {
		assertThat(isTestingConnectionStates).containsExactly(false, true, false)
	}

	@Test
	fun `then the saved library is correct`() {
		assertThat(savedLibrarySettings).isEqualTo(LibrarySettings(
			libraryId = libraryId,
			libraryName = "left",
			syncedFileLocation = SyncedFileLocation.EXTERNAL,
			isUsingExistingFiles = false,
			connectionSettings = StoredMediaCenterConnectionSettings(
				accessCode = "V68Bp9rS",
				isLocalOnly = true,
				isSyncLocalConnectionsOnly = false,
				isWakeOnLanEnabled = true,
				userName = "xw9wy0T",
				password = "sl0Ha",
				macAddress = "sVU0zPNKdFu",
				sslCertificateFingerprint = "",
			)
		))
	}
}
