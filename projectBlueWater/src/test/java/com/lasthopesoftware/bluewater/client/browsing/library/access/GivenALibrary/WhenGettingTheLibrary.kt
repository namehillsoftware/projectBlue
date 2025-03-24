package com.lasthopesoftware.bluewater.client.browsing.library.access.GivenALibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.SyncedFileLocation
import com.lasthopesoftware.bluewater.client.browsing.library.repository.libraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenGettingTheLibrary {

    companion object {
        private val expectedLibrary by lazy {
            Library(
				libraryName = "SomeName",
				isUsingExistingFiles = true,
                nowPlayingId = 14,
				nowPlayingProgress = 80000000000000L,
				isRepeating = true,
				savedTracksString = "This is not even a real track string",
				connectionSettings = Json.encodeToString(
					StoredMediaCenterConnectionSettings(
						accessCode = "aCxeS",
						isSyncLocalConnectionsOnly = true,
						password = "somePass",
						userName = "myUser",
						isWakeOnLanEnabled = true,
						syncedFileLocation = SyncedFileLocation.EXTERNAL,
					)
				),
			)
        }

        private val retrievedLibrary by lazy {
			val libraryRepository = LibraryRepository(ApplicationProvider.getApplicationContext())
			libraryRepository
				.saveLibrary(expectedLibrary)
				.eventually { l ->
					expectedLibrary.id = l.id
					libraryRepository.promiseLibrary(l.libraryId)
				}
				.toExpiringFuture()
				.get()
		}
    }

	@Test
	fun thenTheLibraryIsCorrect() {
		assertThat(retrievedLibrary).isEqualTo(expectedLibrary)
	}
}
