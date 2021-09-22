package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeLibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class WhenGettingAllLibraries {
	@Test
	fun thenTheLibraryIsNotLocalOnly() {
		assertThat(libraries).containsExactlyElementsOf(expectedLibraries)
	}

	companion object {
		private val expectedLibraries: Collection<Library> = Arrays.asList(
			Library().setId(5).setIsSyncLocalConnectionsOnly(true).setLocalOnly(true),
			Library().setId(4),
			Library().setId(8),
			Library().setId(99).setIsSyncLocalConnectionsOnly(true).setLocalOnly(true),
			Library().setId(13)
		)
		private var libraries: Collection<Library>? = null

		@BeforeClass
		@JvmStatic
		fun context() {
			val syncLibraryProvider = SyncLibraryProvider(
				FakeLibraryProvider(
					Library().setId(5).setIsSyncLocalConnectionsOnly(true),
					Library().setId(4),
					Library().setId(8),
					Library().setId(99).setIsSyncLocalConnectionsOnly(true),
					Library().setId(13)
				)
			)
			libraries = FuturePromise(syncLibraryProvider.allLibraries).get()
		}
	}
}
