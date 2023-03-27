package com.lasthopesoftware.bluewater.client.browsing.library.access.GivenALibrary

import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library.ViewType
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WhenGettingTheLibrary {

    companion object {
        private val expectedLibrary by lazy {
            Library()
                .setLibraryName("SomeName")
                .setAccessCode("aCxeS")
                .setCustomSyncedFilesPath("custom")
                .setIsSyncLocalConnectionsOnly(true)
                .setIsUsingExistingFiles(true)
                .setIsWakeOnLanEnabled(true)
                .setLocalOnly(true)
                .setNowPlayingId(14)
                .setNowPlayingProgress(80000000000000L)
                .setPassword("somePass")
                .setUserName("myUser")
                .setSelectedView(32)
                .setSelectedViewType(ViewType.StandardServerView)
                .setRepeating(true)
                .setSavedTracksString("This is not even a real track string")
                .setSyncedFileLocation(Library.SyncedFileLocation.CUSTOM)
        }

        private val retrievedLibrary by lazy {
			val libraryRepository = LibraryRepository(ApplicationProvider.getApplicationContext())
			libraryRepository
				.saveLibrary(expectedLibrary)
				.eventually { l -> libraryRepository.promiseLibrary(l.libraryId) }
				.toExpiringFuture()
				.get()
		}
    }

	@Test
	fun thenTheLibraryIsCorrect() {
		assertThat(retrievedLibrary).isEqualTo(expectedLibrary)
	}
}
