package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.ExpiringFuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingALibraryThatDoesSyncsOnAnyConnection {
	private val library by lazy {
		val syncLibraryProvider = SyncLibraryProvider(
			FakeLibraryRepository(
				Library().setId(3),
				Library().setId(4),
				Library().setId(8).setIsSyncLocalConnectionsOnly(true),
				Library().setId(1),
				Library().setId(13).setIsSyncLocalConnectionsOnly(true)
			)
		)
		ExpiringFuturePromise(syncLibraryProvider.promiseLibrary(LibraryId(4))).get()
	}

    @Test
    fun `then the library is not local only`() {
        assertThat(library!!.isLocalOnly).isFalse
    }
}
