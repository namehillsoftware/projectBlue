package com.lasthopesoftware.bluewater.client.connection.lookup.subsonic.GivenAUrl

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When getting the server info` {
	companion object {
		private const val libraryId = 254
	}

	@OptIn(ExperimentalStdlibApi::class)
	private val services by lazy {
		ServerLookup(
			mockk {
				every { promiseConnectionSettings(LibraryId(libraryId)) } returns SubsonicConnectionSettings(
					url = "http://0IsPoFSrIQ:316",
					userName = "u7OY472Smvf",
					password = "G5gVek1fn",
					isWakeOnLanEnabled = false,
					macAddress = "E5:F1:36:B7:D3:75",
					sslCertificateFingerprint = "79786347fca14302a8ce96f586b17af8".hexToByteArray()
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
		assertThat(serverInfo?.remoteHosts).isEqualTo(setOf("0IsPoFSrIQ"))
	}

	@Test
	fun `then the http port is correct`() {
		assertThat(serverInfo!!.httpPort).isEqualTo(316)
	}

	@Test
	fun `then the https port is correct`() {
		assertThat(serverInfo?.httpsPort).isNull()
	}

	@Test
	fun `then the local hosts are correct`() {
		assertThat(serverInfo?.localHosts).isEmpty()
	}

	@Test
	fun `then the mac addresses are correct`() {
		assertThat(serverInfo?.macAddresses).containsExactlyInAnyOrder(
			"E5:F1:36:B7:D3:75",
		)
	}

	@OptIn(ExperimentalStdlibApi::class)
	@Test
	fun `then the ssl certificate is correct`() {
		assertThat(serverInfo?.certificateFingerprint).isEqualTo("79786347fca14302a8ce96f586b17af8".hexToByteArray())
	}
}
