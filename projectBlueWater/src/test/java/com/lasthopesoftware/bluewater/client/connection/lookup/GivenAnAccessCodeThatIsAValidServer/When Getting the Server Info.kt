package com.lasthopesoftware.bluewater.client.connection.lookup.GivenAnAccessCodeThatIsAValidServer

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 968

class `When Getting the Server Info` {
	private val services by lazy {
		ServerLookup(
			mockk {
				every { lookupConnectionSettings(LibraryId(libraryId)) } returns MediaCenterConnectionSettings(
					accessCode = "http://gooPc:80",
					macAddress = "5A:D2:D4:F6:D0:4A",
				).toPromise()
			},
			mockk {
				every { promiseServerInfoXml(LibraryId(libraryId)) } returns Promise.empty()
			},
		)
	}

	private var serverInfo: ServerInfo? = null

	@BeforeAll
	fun act() {
		serverInfo = services.promiseServerInformation(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the remote host is correct`() {
		assertThat(serverInfo?.remoteHost).isEqualTo("gooPc")
	}

	@Test
	fun `then the http port is correct`() {
		assertThat(serverInfo!!.httpPort).isEqualTo(80)
	}

	@Test
	fun `then the https port is correct`() {
		assertThat(serverInfo?.httpsPort).isNull()
	}

	@Test
	fun `then the local hosts are correct`() {
		assertThat(serverInfo?.localIps).isEmpty()
	}

	@Test
	fun `then the mac addresses are correct`() {
		assertThat(serverInfo?.macAddresses).containsExactlyInAnyOrder(
			"5A:D2:D4:F6:D0:4A",
		)
	}
}
