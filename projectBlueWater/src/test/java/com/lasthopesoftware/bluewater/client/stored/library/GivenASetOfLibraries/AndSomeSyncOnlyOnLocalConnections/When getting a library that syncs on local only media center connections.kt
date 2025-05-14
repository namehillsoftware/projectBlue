package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When getting a library that syncs on local only media center connections` {
	private val library by lazy {
		val syncLibraryProvider = SyncLibraryConnectionSettings(
			mockk {
				every { promiseLibrarySettings(LibraryId(8)) } returns LibrarySettings(
					connectionSettings = StoredMediaCenterConnectionSettings(
						accessCode = "9d8dIQP0",
						isSyncLocalConnectionsOnly = true,
                	)
				).toPromise()
			}
		)

		syncLibraryProvider.promiseConnectionSettings(LibraryId(8)).toExpiringFuture().get() as? MediaCenterConnectionSettings
	}

	@Test
	fun `then the library is local only`() {
		assertThat(library?.isLocalOnly).isTrue
	}
}
