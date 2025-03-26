package com.lasthopesoftware.bluewater.client.connection.lookup.mediacenter.GivenAnAccessCodeThatIsAValidServer.AndAnHttpsScheme

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 706

class `When Getting the Server Info` {
	private val services by lazy {
        ServerLookup(
            mockk {
                every { promiseConnectionSettings(LibraryId(com.lasthopesoftware.bluewater.client.connection.lookup.mediacenter.GivenAnAccessCodeThatIsAValidServer.AndAnHttpsScheme.libraryId)) } returns MediaCenterConnectionSettings(
                    accessCode = "https://b7XpyOQv:63389",
					sslCertificateFingerprint = Hex.decodeHex("d0a3e4bf62221422a2e5dc9c479c2b36")
                ).toPromise()
            },
            mockk {
				every { promiseServerInfoXml(LibraryId(com.lasthopesoftware.bluewater.client.connection.lookup.mediacenter.GivenAnAccessCodeThatIsAValidServer.AndAnHttpsScheme.libraryId)) } returns Promise.empty()
			},
        )
	}

	private var serverInfo: ServerInfo? = null

	@BeforeAll
	fun act() {
		serverInfo = services.promiseServerInformation(LibraryId(com.lasthopesoftware.bluewater.client.connection.lookup.mediacenter.GivenAnAccessCodeThatIsAValidServer.AndAnHttpsScheme.libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the server info is correct`() {
		assertThat(serverInfo).isEqualTo(
            ServerInfo(
			httpPort = null,
			httpsPort = 63389,
			remoteHosts = setOf("b7XpyOQv"),
			macAddresses = emptySet(),
			localHosts = emptySet(),
			certificateFingerprint = Hex.decodeHex("d0a3e4bf62221422a2e5dc9c479c2b36"),
		)
        )
	}
}
