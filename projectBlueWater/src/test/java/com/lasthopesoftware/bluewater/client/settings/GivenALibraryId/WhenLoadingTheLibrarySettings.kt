package com.lasthopesoftware.bluewater.client.settings.GivenALibraryId

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingTheLibrarySettings {

    private val libraryId = LibraryId(659)

    private val services by lazy {
        LibrarySettingsViewModel(
            FakeLibraryProvider(
                Library(
                    _id = libraryId.id,
                    _accessCode = "r64HLI",
                    _customSyncedFilesPath = "T5YN",
                    _isLocalOnly = true,
                    _isSyncLocalConnectionsOnly = true,
                    _isWakeOnLanEnabled = true,
                    _userName = "ZaxM5Iid",
                    _password = "sL33L3Xt",
                    _syncedFileLocation = Library.SyncedFileLocation.EXTERNAL,
                    _isUsingExistingFiles = true,
                )
            ),
            mockk(),
            mockk(),
        )
    }

    @BeforeAll
    fun act() {
        services.loadLibrary(libraryId).toExpiringFuture().get()
    }

    @Test
    fun `then the access code is correct`() {
        Assertions.assertThat(services.accessCode.value).isEqualTo("r64HLI")
    }

    @Test
    fun `then the custom synced files path is correct`() {
        Assertions.assertThat(services.customSyncPath.value).isEqualTo("T5YN")
    }

    @Test
    fun `then the connection is local only`() {
        Assertions.assertThat(services.isLocalOnly.value).isTrue
    }

    @Test
    fun `then sync local only connections is correct`() {
        Assertions.assertThat(services.isSyncLocalConnectionsOnly.value).isTrue
    }

    @Test
    fun `then wake on lan is correct`() {
        Assertions.assertThat(services.isWakeOnLanEnabled.value).isTrue
    }

    @Test
    fun `then the user name is correct`() {
        Assertions.assertThat(services.userName.value).isEqualTo("ZaxM5Iid")
    }

    @Test
    fun `then the password is correct`() {
        Assertions.assertThat(services.password.value).isEqualTo("sL33L3Xt")
    }

    @Test
    fun `then synced file location is correct`() {
        Assertions.assertThat(services.syncedFileLocation.value)
            .isEqualTo(Library.SyncedFileLocation.EXTERNAL)
    }

    @Test
    fun `then is using existing files is correct`() {
        Assertions.assertThat(services.isUsingExistingFiles.value).isTrue
    }
}
