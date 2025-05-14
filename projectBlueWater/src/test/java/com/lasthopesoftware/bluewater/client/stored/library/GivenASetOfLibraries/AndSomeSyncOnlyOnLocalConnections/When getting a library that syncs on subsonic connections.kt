package com.lasthopesoftware.bluewater.client.stored.library.GivenASetOfLibraries.AndSomeSyncOnlyOnLocalConnections

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.stored.library.SyncLibraryConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class `When getting a library that syncs on subsonic connections` {
	private val connectionSettings by lazy {
		val syncLibraryProvider = SyncLibraryConnectionSettings(
			mockk {
				every { promiseLibrarySettings(LibraryId(8)) } returns LibrarySettings(
					connectionSettings = StoredSubsonicConnectionSettings(
						url = "pxLGAQo89",
						userName = "b1UJSCEj",
						password = "XXZfQGRtC0j",
                	)
				).toPromise()
			}
		)

		syncLibraryProvider.promiseConnectionSettings(LibraryId(8)).toExpiringFuture().get() as? SubsonicConnectionSettings
	}

	@Test
	fun `then the connection is returned`() {
		assertThat(connectionSettings).isEqualTo(
			SubsonicConnectionSettings(
				url = "pxLGAQo89",
				userName = "b1UJSCEj",
				password = "XXZfQGRtC0j",
			)
		)
	}
}
