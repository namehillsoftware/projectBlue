package com.lasthopesoftware.bluewater.client.settings.GivenAnUnknownLibraryId

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.strings.FakeStringResources
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
					connectionSettings = StoredSubsonicConnectionSettings(
						url = "yKV48o",
						isWakeOnLanEnabled = true,
						password = "7t5nHd",
					),
				).toPromise()
			},
			mockk(),
			mockk(),
			mockk(),
			mockk(),
			FakeStringResources(),
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
	fun `then the connection settings are correct`() {
		assertThat(services.connectionSettingsViewModel.value).isNull()
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
