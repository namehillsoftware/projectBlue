package com.lasthopesoftware.bluewater.client.connection.settings

import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenGettingServerMediaCenterConnectionSettings {

	private val mut by lazy {
		val libraryProvider = mockk<ILibraryProvider>()
		every { libraryProvider.promiseLibrary(LibraryId(10)) } returns Library(
			connectionSettings = Json.encodeToString(
				StoredMediaCenterConnectionSettings(
					accessCode = "P9qd",
					userName = "x39",
					isLocalOnly = true,
					password = "gEaP",
					sslCertificateFingerprint = "A10B",
					isWakeOnLanEnabled = true,
				)
			)
		).toPromise()

		val connectionSettingsLookup = ConnectionSettingsLookup(libraryProvider)

		connectionSettingsLookup
	}

	private var connectionSettings: MediaCenterConnectionSettings? = null

	@BeforeAll
	fun act() {
		connectionSettings = mut.lookupConnectionSettings(LibraryId(10)).toExpiringFuture().get()
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
