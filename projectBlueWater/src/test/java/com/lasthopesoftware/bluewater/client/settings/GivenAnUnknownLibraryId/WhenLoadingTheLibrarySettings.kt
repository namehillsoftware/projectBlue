package com.lasthopesoftware.bluewater.client.settings.GivenAnUnknownLibraryId

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingTheLibrarySettings {

    private val libraryId = LibraryId(236)

    private val services by lazy {
        LibrarySettingsViewModel(
            FakeLibraryRepository(
                Library(
                    _id = libraryId.id,
                    _accessCode = "yKV48o",
                    _customSyncedFilesPath = "3RrtFh",
                    _isLocalOnly = true,
                    _isSyncLocalConnectionsOnly = true,
                    _isWakeOnLanEnabled = true,
                    _userName = "6O6Wkha6",
                    _password = "7t5nHd",
                    _syncedFileLocation = Library.SyncedFileLocation.EXTERNAL,
                    _isUsingExistingFiles = true,
                )
            ),
            mockk(),
            mockk(),
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
    fun `then the custom synced files path is correct`() {
        assertThat(services.customSyncPath.value).isEqualTo("")
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
            .isEqualTo(Library.SyncedFileLocation.INTERNAL)
    }

    @Test
    fun `then is using existing files is correct`() {
        assertThat(services.isUsingExistingFiles.value).isFalse
    }
}
