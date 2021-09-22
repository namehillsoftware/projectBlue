package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenGettingALibraryThatDoesSyncsOnAnyConnection {
    @Test
    fun thenTheLibraryIsNotLocalOnly() {
        assertThat(library!!.isLocalOnly).isFalse
    }

    companion object {
        private var library: Library? = null
        @BeforeClass
		@JvmStatic
        fun context() {
            val syncLibraryProvider = SyncLibraryProvider(
                FakeLibraryProvider(
                    Library().setId(3),
                    Library().setId(4),
                    Library().setId(8).setIsSyncLocalConnectionsOnly(true),
                    Library().setId(1),
                    Library().setId(13).setIsSyncLocalConnectionsOnly(true)
                )
            )
            library = FuturePromise(syncLibraryProvider.getLibrary(LibraryId(4))).get()
        }
    }
}
