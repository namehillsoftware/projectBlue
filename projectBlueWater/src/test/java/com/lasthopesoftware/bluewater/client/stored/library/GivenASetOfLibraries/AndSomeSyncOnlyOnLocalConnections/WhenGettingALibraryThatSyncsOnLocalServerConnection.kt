package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingALibraryThatSyncsOnLocalServerConnection {
	private val library by lazy {
		val syncLibraryProvider = SyncLibraryProvider(
			FakeLibraryRepository(
				Library(id = 3),
				Library(id = 4),
				Library(id = 8, isSyncLocalConnectionsOnly = true),
				Library(id = 1),
				Library(id = 13, isSyncLocalConnectionsOnly = true)
			)
		)
		ExpiringFuturePromise(syncLibraryProvider.promiseLibrary(LibraryId(8))).get()
	}

	@Test
	fun `then the library is local only`() {
		assertThat(library!!.isLocalOnly).isTrue
	}
}
