package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingServerMediaCenterConnectionSettings {

	@OptIn(ExperimentalStdlibApi::class)
	private val mut by lazy {
		val connectionSettingsLookup = ValidConnectionSettingsLookup(mockk {
			every { promiseConnectionSettings(LibraryId(10)) } returns MediaCenterConnectionSettings(
				accessCode = "P9qd",
				userName = "x39",
				isLocalOnly = true,
				password = "gEaP",
				sslCertificateFingerprint = "A10B".hexToByteArray(),
				isWakeOnLanEnabled = true,
			).toPromise()
		})

		connectionSettingsLookup
	}

	private var connectionSettings: MediaCenterConnectionSettings? = null

	@BeforeAll
	fun act() {
		connectionSettings = mut.promiseConnectionSettings(LibraryId(10)).toExpiringFuture().get()
	}

	@Test
	fun `then the connection settings are correct`() {
		assertThat(connectionSettings).isEqualTo(
			MediaCenterConnectionSettings(
				accessCode = "P9qd",
				userName = "x39",
				password = "gEaP",
				sslCertificateFingerprint = Hex.decodeHex("A10B"),
				isWakeOnLanEnabled = true,
				isLocalOnly = true,
			)
		)
	}
}
