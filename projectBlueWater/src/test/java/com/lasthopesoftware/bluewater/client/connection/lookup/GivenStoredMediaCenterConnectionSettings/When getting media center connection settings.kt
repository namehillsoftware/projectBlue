package com.lasthopesoftware.bluewater.client.connection.lookup.GivenStoredMediaCenterConnectionSettings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.StoredMediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting media center connection settings` {

	companion object {
		private const val libraryId = 562
	}

	private val services by lazy {
		ConnectionSettingsLookup(
			mockk {
				every { promiseLibrarySettings(LibraryId(libraryId)) } returns LibrarySettings(
					libraryId = LibraryId(libraryId),
					connectionSettings = StoredMediaCenterConnectionSettings(
						isLocalOnly = true,
						isWakeOnLanEnabled = true,
						password = "6r8hgJ8x",
						userName = "NYS1VeeiiE",
						accessCode = "CfRnKe4Y",
						macAddress = "JJGehSnl",
						sslCertificateFingerprint = "3f34c3f7fda04081bae8b5181a48fffd",
					)
				).toPromise()
			},
			mockk(),
			mockk(),
		)
	}

	private var connectionsSettings: MediaCenterConnectionSettings? = null

	@BeforeAll
	fun act() {
		connectionsSettings = services.promiseConnectionSettings(LibraryId(libraryId)).toExpiringFuture().get() as? MediaCenterConnectionSettings
	}

	@Test
	@OptIn(ExperimentalStdlibApi::class)
	fun `then the connection settings are correct`() {
		assertThat(connectionsSettings).isEqualTo(
			MediaCenterConnectionSettings(
				macAddress = "JJGehSnl",
				isWakeOnLanEnabled = true,
				password = "6r8hgJ8x",
				userName = "NYS1VeeiiE",
				accessCode = "CfRnKe4Y",
				isLocalOnly = true,
				sslCertificateFingerprint = "3f34c3f7fda04081bae8b5181a48fffd".hexToByteArray()
			)
		)
	}
}
