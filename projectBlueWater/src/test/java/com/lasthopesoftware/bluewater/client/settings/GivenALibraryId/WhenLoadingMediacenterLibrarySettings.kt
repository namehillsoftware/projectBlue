package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.observables.mapNotNull
import com.lasthopesoftware.bluewater.shared.observables.toCloseable
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingMediacenterLibrarySettings {

    private val libraryId = LibraryId(659)

    private val services by lazy {
        LibrarySettingsViewModel(
			mockk {
				every { promiseLibrarySettings(libraryId) } returns LibrarySettings(
					libraryId = libraryId,
					libraryName = "L7bXDhiE",
					isUsingExistingFiles = true,
					syncedFileLocation = SyncedFileLocation.INTERNAL,
					connectionSettings = StoredMediaCenterConnectionSettings(
						userName = "YzLZsqm",
						accessCode = "qO9x15St",
						isWakeOnLanEnabled = false,
						password = "3YDQN9mMLe",
					)
				).toPromise()
			},
			mockk(),
			mockk(),
			mockk(),
			FakeStringResources(),
		)
    }

	private val connectionSettingsViewModel
		get() = services.connectionSettingsViewModel.value as? LibrarySettingsViewModel.MediaCenterConnectionSettingsViewModel
	private var isSettingsChangedAtEnd = false

    @BeforeAll
    fun act() {
		services.isSettingsChanged.mapNotNull().subscribe { isSettingsChangedAtEnd = it }.toCloseable().use {
			services.loadLibrary(libraryId).toExpiringFuture().get()
		}
    }

	@Test
	fun `then the settings are not changed`() {
		assertThat(isSettingsChangedAtEnd).isFalse
	}

    @Test
    fun `then the url is correct`() {
        assertThat(connectionSettingsViewModel?.accessCode?.value).isEqualTo("qO9x15St")
    }

    @Test
    fun `then wake on lan is correct`() {
        assertThat(connectionSettingsViewModel?.isWakeOnLanEnabled?.value).isFalse
    }

    @Test
    fun `then the user name is correct`() {
        assertThat(connectionSettingsViewModel?.userName?.value).isEqualTo("YzLZsqm")
    }

    @Test
    fun `then the password is correct`() {
        assertThat(connectionSettingsViewModel?.password?.value).isEqualTo("3YDQN9mMLe")
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
		assertThat(services.libraryName.value).isEqualTo("L7bXDhiE")
	}

	@Test
	fun `then the mac address is correct`() {
		assertThat(connectionSettingsViewModel?.macAddress?.value).isEmpty()
	}
}
