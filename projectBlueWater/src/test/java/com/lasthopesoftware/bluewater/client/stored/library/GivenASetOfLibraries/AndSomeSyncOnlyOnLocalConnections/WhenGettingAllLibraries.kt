package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingAllLibraries {
	private val expectedLibraries = listOf(
		Library().setId(5).setIsSyncLocalConnectionsOnly(true).setLocalOnly(true),
		Library().setId(4),
		Library().setId(8),
		Library().setId(99).setIsSyncLocalConnectionsOnly(true).setLocalOnly(true),
		Library().setId(13)
	)

	private val libraries by lazy {
		val syncLibraryProvider = SyncLibraryProvider(
			FakeLibraryProvider(
				Library().setId(5).setIsSyncLocalConnectionsOnly(true),
				Library().setId(4),
				Library().setId(8),
				Library().setId(99).setIsSyncLocalConnectionsOnly(true),
				Library().setId(13)
			)
		)
		ExpiringFuturePromise(syncLibraryProvider.allLibraries).get()
	}

	@Test
	fun `then the libraries are correct`() {
		assertThat(libraries).containsExactlyElementsOf(expectedLibraries)
	}
}
