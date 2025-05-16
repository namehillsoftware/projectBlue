package com.lasthopesoftware.bluewater.client.connection.settings.subsonic.GivenALibraryWithoutAUrl

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.MissingAccessCodeException
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ValidConnectionSettingsLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class `When getting valid subsonic connection settings` {
	companion object {
		private const val libraryId = 158
	}

	@OptIn(ExperimentalStdlibApi::class)
	private val mutt by lazy {
		ValidConnectionSettingsLookup(
			mockk {
				every { promiseConnectionSettings(LibraryId(libraryId)) } returns SubsonicConnectionSettings(
					url = "",
					userName = "Gl3lZxB",
					password = "G5gVek1fn",
					isWakeOnLanEnabled = true,
					sslCertificateFingerprint = "7edb15f46067453f99f0de050c94076e".hexToByteArray(),
					macAddress = "D5:38:83:8D:EE:7B"
				).toPromise()
			}
		)
	}

	private var exception: MissingAccessCodeException? = null

	@BeforeAll
	fun act() {
		try {
			mutt.promiseConnectionSettings(LibraryId(libraryId)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? MissingAccessCodeException
		}
	}

	@Test
	fun `then a missing access code exception is thrown`() {
		assertThat(exception).isNotNull
	}
}
