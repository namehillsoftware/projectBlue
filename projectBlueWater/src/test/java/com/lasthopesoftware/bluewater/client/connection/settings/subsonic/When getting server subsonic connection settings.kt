package com.lasthopesoftware.bluewater.client.connection.settings.subsonic

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredSubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting server subsonic connection settings` {
	companion object {
		private const val libraryId = 529
	}

	private val mutt by lazy {
		ConnectionSettingsLookup(
			mockk {
				every { promiseLibrarySettings(LibraryId(libraryId)) } returns LibrarySettings(
					libraryId = LibraryId(libraryId),
					connectionSettings = StoredSubsonicConnectionSettings(
						url = "XkBYgZZXtG",
						userName = "Gl3lZxB",
						password = "G5gVek1fn",
						isWakeOnLanEnabled = true,
						sslCertificateFingerprint = "df6a6e32bdb440d0949b93a89796ac9e",
						macAddress = "D5:38:83:8D:EE:7B",
					)
				).toPromise()
			},
			mockk(),
			mockk(),
		)
	}

	private var connectionSettings: ConnectionSettings? = null

	@BeforeAll
	fun act() {
		connectionSettings = mutt.promiseConnectionSettings(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@OptIn(ExperimentalStdlibApi::class)
	@Test
	fun `then the connection settings are correct`() {
		assertThat(connectionSettings).isEqualTo(
			SubsonicConnectionSettings(
				url = "XkBYgZZXtG",
				userName = "Gl3lZxB",
				password = "G5gVek1fn",
				isWakeOnLanEnabled = true,
				sslCertificateFingerprint = "df6a6e32bdb440d0949b93a89796ac9e".hexToByteArray(),
				macAddress = "D5:38:83:8D:EE:7B"
			)
		)
	}
}
