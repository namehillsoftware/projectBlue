package com.lasthopesoftware.bluewater.client.connection.settings.subsonic.GivenALibraryWithValidSettings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidConnectionSettingsLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting valid subsonic connection settings` {
	companion object {
		private const val libraryId = 529
	}

	@OptIn(ExperimentalStdlibApi::class)
	private val mutt by lazy {
		ValidConnectionSettingsLookup(
			mockk {
				every { promiseConnectionSettings(LibraryId(libraryId)) } returns SubsonicConnectionSettings(
					url = "U2dvnR4u",
					userName = "kDWTAZa9t7",
					password = "JNkXq5l",
					isWakeOnLanEnabled = false,
					sslCertificateFingerprint = "7edb15f46067453f99f0de050c94076e".hexToByteArray(),
					macAddress = "D5:38:83:8D:EE:7B"
				).toPromise()
			}
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
				url = "U2dvnR4u",
				userName = "kDWTAZa9t7",
				password = "JNkXq5l",
				isWakeOnLanEnabled = false,
				sslCertificateFingerprint = "7edb15f46067453f99f0de050c94076e".hexToByteArray(),
				macAddress = "D5:38:83:8D:EE:7B"
			)
		)
	}
}
