package com.lasthopesoftware.bluewater.client.settings.GivenAnUnknownLibraryId

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingTheLibrarySettings {

    private val libraryId = LibraryId(236)

    private val services by lazy {
        LibrarySettingsViewModel(
			mockk {
				every { promiseLibrarySettings(any()) } returns Promise.empty()
				every { promiseLibrarySettings(libraryId) } returns LibrarySettings(
					libraryId = libraryId,
					isUsingExistingFiles = true,
					syncedFileLocation = SyncedFileLocation.EXTERNAL,
					connectionSettings = StoredMediaCenterConnectionSettings(
						accessCode = "yKV48o",
						isLocalOnly = true,
						isSyncLocalConnectionsOnly = true,
						isWakeOnLanEnabled = true,
						password = "7t5nHd",
					),
				).toPromise()
			},
			mockk(),
			mockk(),
			mockk(),
		)
    }

    @BeforeAll
    fun act() {
		with (services) {
			loadLibrary(libraryId).toExpiringFuture().get()
			loadLibrary(LibraryId(-1)).toExpiringFuture().get()
		}
    }

    @Test
    fun `then the access code is correct`() {
        assertThat(services.accessCode.value).isEqualTo("")
    }

    @Test
    fun `then the connection is local only`() {
        assertThat(services.isLocalOnly.value).isFalse
    }

    @Test
    fun `then sync local only connections is correct`() {
        assertThat(services.isSyncLocalConnectionsOnly.value).isFalse
    }

    @Test
    fun `then wake on lan is correct`() {
        assertThat(services.isWakeOnLanEnabled.value).isFalse
    }

    @Test
    fun `then the user name is correct`() {
        assertThat(services.userName.value).isEqualTo("")
    }

    @Test
    fun `then the password is correct`() {
        assertThat(services.password.value).isEqualTo("")
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
}
