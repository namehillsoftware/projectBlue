package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfStoredItems

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.TimeUnit

class WhenCollectingTheAssociatedServiceFiles {
    companion object {
        private val collectedFiles by lazy {
			val storedItemAccess: AccessStoredItems = FakeStoredItemAccess()
			val fileProvider = Mockito.mock(
				ProvideLibraryFiles::class.java
			)
			val serviceFileCollector = StoredItemServiceFileCollector(
				storedItemAccess,
				fileProvider,
				FileListParameters
			)
			serviceFileCollector.promiseServiceFilesToSync(LibraryId(14)).toFuture()[1, TimeUnit.SECONDS]
		}
    }

	@Test
	fun thenNoServiceFilesAreReturned() {
		assertThat(collectedFiles).isEmpty()
	}
}
