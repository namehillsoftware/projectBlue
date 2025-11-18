package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId

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

class WhenLoadingSubsonicLibrarySettings {

    private val libraryId = LibraryId(659)

    private val services by lazy {
        LibrarySettingsViewModel(
			mockk {
				every { promiseLibrarySettings(libraryId) } returns LibrarySettings(
					libraryId = libraryId,
					isUsingExistingFiles = true,
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					connectionSettings = StoredSubsonicConnectionSettings(
						userName = "ZaxM5Iid",
						url = "r64HLI",
						isWakeOnLanEnabled = true,
						password = "sL33L3Xt",
					)
				).toPromise()
			},
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			FakeStringResources(),
		)
    }

	private val connectionSettingsViewModel
		get() = services.connectionSettingsViewModel.value as? LibrarySettingsViewModel.SubsonicConnectionSettingsViewModel

    @BeforeAll
    fun act() {
        services.loadLibrary(libraryId).toExpiringFuture().get()
    }

	@Test
	fun `then the settings are not changed`() {
		assertThat(services.isSettingsChanged.value).isFalse
	}

    @Test
    fun `then the url is correct`() {
        assertThat(connectionSettingsViewModel?.url?.value).isEqualTo("r64HLI")
    }

    @Test
    fun `then wake on lan is correct`() {
        assertThat(connectionSettingsViewModel?.isWakeOnLanEnabled?.value).isTrue
    }

    @Test
    fun `then the user name is correct`() {
        assertThat(connectionSettingsViewModel?.userName?.value).isEqualTo("ZaxM5Iid")
    }

    @Test
    fun `then the password is correct`() {
        assertThat(connectionSettingsViewModel?.password?.value).isEqualTo("sL33L3Xt")
    }

    @Test
    fun `then synced file location is correct`() {
        assertThat(services.syncedFileLocation.value)
            .isEqualTo(SyncedFileLocation.EXTERNAL)
    }

    @Test
    fun `then is using existing files is correct`() {
        assertThat(services.isUsingExistingFiles.value).isTrue
    }

	@Test
	fun `then the library name is correct`() {
		assertThat(services.libraryName.value).isEmpty()
	}

	@Test
	fun `then the mac address is correct`() {
		assertThat(connectionSettingsViewModel?.macAddress?.value).isEmpty()
	}
}
