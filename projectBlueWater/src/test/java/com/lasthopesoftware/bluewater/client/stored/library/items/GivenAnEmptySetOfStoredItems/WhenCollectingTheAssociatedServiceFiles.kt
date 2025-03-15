package com.lasthopesoftware.bluewater.client.stored.library.items.GivenAnEmptySetOfStoredItems

import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideLibraryFiles
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemServiceFileCollector
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class WhenCollectingTheAssociatedServiceFiles {
	private val collectedFiles by lazy {
		val storedItemAccess = FakeStoredItemAccess()
		val fileProvider = mockk<ProvideLibraryFiles>()
		val serviceFileCollector = StoredItemServiceFileCollector(
            storedItemAccess,
            fileProvider
        )
		serviceFileCollector.promiseServiceFilesToSync(LibraryId(14)).toExpiringFuture()[1, TimeUnit.SECONDS]
	}

	@Test
	fun `then no service files are returned`() {
		assertThat(collectedFiles).isEmpty()
	}
}
