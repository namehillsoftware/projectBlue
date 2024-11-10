package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenNoServerInfoXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val libraryId = 601

class WhenParsingTheServerInfo {

	private val serverInfo by lazy {
		val serverLookup = ServerLookup(
			mockk {
				every { lookupConnectionSettings(LibraryId(libraryId)) } returns Promise(
					ConnectionSettings(
						accessCode = "JcMHkVt5mty",
						sslCertificateFingerprint = Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45")
					)
				)
			},
			mockk {
				every { promiseServerInfoXml(any()) } returns Promise.empty()
			})
		serverLookup.promiseServerInformation(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the server info is correct`() {
		assertThat(serverInfo).isEqualTo(
			ServerInfo(
				httpsPort = 443,
				remoteHost = "JcMHkVt5mty",
				certificateFingerprint = Hex.decodeHex("2386166660562C5AAA1253B2BED7C2483F9C2D45"),
			)
		)
	}
}
