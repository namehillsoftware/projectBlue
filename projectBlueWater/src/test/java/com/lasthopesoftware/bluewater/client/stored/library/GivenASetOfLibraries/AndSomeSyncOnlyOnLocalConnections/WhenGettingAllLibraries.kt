package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingAllLibraries {
	private val expectedLibraries = listOf(
		Library(id = 5, isSyncLocalConnectionsOnly = true, isLocalOnly = true),
		Library(id = 4),
		Library(id = 8),
		Library(id = 99, isSyncLocalConnectionsOnly = true, isLocalOnly = true),
		Library(id = 13)
	)

	private val libraries by lazy {
		val syncLibraryProvider = SyncLibraryProvider(
			FakeLibraryRepository(
				Library(id = 5, isSyncLocalConnectionsOnly = true),
				Library(id = 4),
				Library(id = 8),
				Library(id = 99, isSyncLocalConnectionsOnly = true),
				Library(id = 13)
			)
		)
		ExpiringFuturePromise(syncLibraryProvider.allLibraries).get()
	}

	@Test
	fun `then the libraries are correct`() {
		assertThat(libraries).containsExactlyElementsOf(expectedLibraries)
	}
}
