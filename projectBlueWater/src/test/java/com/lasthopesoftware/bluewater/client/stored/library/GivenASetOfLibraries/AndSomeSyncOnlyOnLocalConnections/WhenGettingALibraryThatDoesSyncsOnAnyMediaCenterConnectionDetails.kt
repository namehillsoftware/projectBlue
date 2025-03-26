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

class WhenGettingALibraryThatDoesSyncsOnAnyMediaCenterConnectionDetails {
	private val library by lazy {
		val syncLibraryProvider = SyncLibraryConnectionSettings(
			mockk {
				every { promiseLibrarySettings(LibraryId(4)) } returns LibrarySettings(
					connectionSettings = StoredMediaCenterConnectionSettings(accessCode = "2OoO9Vefrb")
				).toPromise()
			}
		)
		syncLibraryProvider.promiseConnectionSettings(LibraryId(4)).toExpiringFuture().get() as? MediaCenterConnectionSettings
	}

    @Test
    fun `then the library is not local only`() {
        assertThat(library?.isLocalOnly).isFalse
    }
}
