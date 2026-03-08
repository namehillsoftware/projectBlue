package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId.AndLibrarySettingAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.mapNotNull
import com.lasthopesoftware.observables.toCloseable
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When Editing Custom Headers` {

	private val libraryId = LibraryId(56)

	private val savedLibrarySettings = mutableListOf<LibrarySettings>()

	private val services by lazy {
        LibrarySettingsViewModel(
			mockk {
				every { promiseLibrarySettings(libraryId) } returns LibrarySettings(
					libraryId = libraryId,
					isUsingExistingFiles = true,
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					libraryName = null,
					connectionSettings = StoredMediaCenterConnectionSettings(
						accessCode = "b2q",
						isWakeOnLanEnabled = false,
						password = "hmpyA",
						macAddress = "S4YhepUHBcj",
						customHeaders = mapOf("iagP03aiL" to "A4gLbzzNiMR", "cD1k4KQFOg" to "iIFwiwNfQQI")
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
			mockk {
				every { promiseIsConnectionActive(libraryId) } returns false.toPromise()
			},
			FakeStringResources(),
		)
    }

	private var isSaved = false
	private val isEditingHeaderStates = mutableListOf<Boolean>()
	private val isHeaderChangedStates = mutableListOf<Boolean>()
	private val editingNewHeaderStates = mutableListOf<Pair<String, String>>()
	private val editingExistingHeaderStates = mutableListOf<Pair<String, String>>()
	private val headerStates = mutableListOf<Map<String, String>>()
	private var isSettingsChangedAfterEditingHeader = false
	private var isSettingsChangedAfterSavingHeader = false
	private var isSettingsChangedAfterSavingSettings = false

    @BeforeAll
    fun act() {
		with (services) {
			loadLibrary(libraryId).toExpiringFuture().get()

			(connectionSettingsViewModel.value as? LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel)?.apply {
				customHeaders.mapNotNull().subscribe(headerStates::add).toCloseable().use {
					isEditingHeader.mapNotNull().subscribe(isEditingHeaderStates::add).toCloseable().use {
						isHeaderChanged.mapNotNull().subscribe(isHeaderChangedStates::add).toCloseable().use {
							Observable.combineLatest(
								editingHeaderKey.mapNotNull(),
								editingHeaderValue.mapNotNull()
							) { k, v -> k to v }.subscribe(editingNewHeaderStates::add).toCloseable().use {
								editHeader("test")
								editingHeaderValue.value = "n4CVuTP"
								isSettingsChangedAfterEditingHeader = isSettingsChanged.value
								saveHeader()
								isSettingsChangedAfterSavingHeader = isSettingsChanged.value
								editingHeaderKey.value = "bob"
								saveHeader()
								cancelHeaderEdit()
							}
						}

						Observable.combineLatest(
							editingHeaderKey.mapNotNull(),
							editingHeaderValue.mapNotNull()
						) { k, v -> k to v }.subscribe(editingExistingHeaderStates::add).toCloseable().use {
							editHeader("iagP03aiL")
							editingHeaderValue.value = "Y98c8vjHr0"
							saveHeader()
						}

						isSaved = saveLibrary().toExpiringFuture().get() == true
						isSettingsChangedAfterSavingSettings = isSettingsChanged.value
					}
				}
			}
		}
    }

	@Test
	fun `then isEditingHeader states is correct`() {
		assertThat(isEditingHeaderStates).isEqualTo(
			listOf(false, true, false, true)
		)
	}

	@Test
	fun `then the header changed states are correct`() {
		assertThat(isHeaderChangedStates).isEqualTo(listOf(false, true, false, true, false))
	}

	@Test
	fun `then the header states are correct`() {
		assertThat(headerStates).isEqualTo(
			listOf(
				mapOf("iagP03aiL" to "A4gLbzzNiMR", "cD1k4KQFOg" to "iIFwiwNfQQI"),
				mapOf("iagP03aiL" to "A4gLbzzNiMR", "cD1k4KQFOg" to "iIFwiwNfQQI", "test" to "n4CVuTP"),
				mapOf("iagP03aiL" to "A4gLbzzNiMR", "cD1k4KQFOg" to "iIFwiwNfQQI"),
				mapOf("iagP03aiL" to "A4gLbzzNiMR", "cD1k4KQFOg" to "iIFwiwNfQQI", "bob" to "n4CVuTP"),
				mapOf("cD1k4KQFOg" to "iIFwiwNfQQI", "bob" to "n4CVuTP"),
				mapOf("iagP03aiL" to "Y98c8vjHr0", "cD1k4KQFOg" to "iIFwiwNfQQI", "bob" to "n4CVuTP"),
			)
		)
	}

	@Test
	fun `then editing a new header is correct`() {
		assertThat(editingNewHeaderStates).isEqualTo(
			listOf(
				"" to "",
				"test" to "",
				"test" to "n4CVuTP",
				"bob" to "n4CVuTP",
				"" to "n4CVuTP",
				"" to "",
			)
		)
	}

	@Test
	fun `then the saved library is correct`() {
		assertThat(savedLibrarySettings).containsExactly(
			LibrarySettings(
				libraryId = libraryId,
				libraryName = "",
				isUsingExistingFiles = true,
				syncedFileLocation = SyncedFileLocation.EXTERNAL,
				connectionSettings = StoredMediaCenterConnectionSettings(
					accessCode = "b2q",
					isWakeOnLanEnabled = false,
					userName = "",
					password = "hmpyA",
					macAddress = "S4YhepUHBcj",
					sslCertificateFingerprint = "",
					customHeaders = mapOf(
						"cD1k4KQFOg" to "iIFwiwNfQQI",
						"bob" to "n4CVuTP",
						"iagP03aiL" to "Y98c8vjHr0"
					),
				)
			)
		)
	}

	@Test
	fun `then the settings are not changed after editing a header`() {
		assertThat(isSettingsChangedAfterEditingHeader).isFalse
	}

	@Test
	fun `then the settings are changed after saving a header`() {
		assertThat(isSettingsChangedAfterSavingHeader).isTrue
	}

	@Test
	fun `then the settings are not changed after saving the settings`() {
		assertThat(isSettingsChangedAfterSavingSettings).isFalse
	}
}
