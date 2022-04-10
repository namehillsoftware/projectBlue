package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenServerInfoXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import xmlwise.Xmlwise

class WhenParsingTheServerInfo {

	companion object {
		private val serverInfo by lazy {
			val serverInfoXml = mockk<RequestServerInfoXml>()
			every { serverInfoXml.promiseServerInfoXml(any()) } returns Promise(
				Xmlwise.createXml(
					"""<?xml version="1.0" encoding="UTF-8"?>
<Response Status="OK">
<keyid>gooPc</keyid>
<ip>108.491.23.154</ip>
<port>52199</port>
<localiplist>169.254.72.216,192.168.1.50</localiplist>
<certificate_fingerprint>746E06046B44CED35658F300DB2D08A799DEBC7E</certificate_fingerprint>
<macaddresslist>
5c-f3-70-8b-db-e9,16-15-f4-b9-cd-15,b4-2e-99-31-f7-eb
</macaddresslist>
<https_port>52200</https_port>
</Response>"""
				)
			)
			val serverLookup = ServerLookup(serverInfoXml)
			serverLookup.promiseServerInformation(LibraryId(10)).toExpiringFuture().get()
		}
	}

	@Test
	fun thenTheRemoteIpIsCorrect() {
		assertThat(serverInfo!!.remoteIp).isEqualTo("108.491.23.154")
	}

	@Test
	fun thenTheLocalIpsAreCorrect() {
		assertThat(serverInfo!!.localIps)
			.containsExactlyInAnyOrder("169.254.72.216", "192.168.1.50")
	}

	@Test
	fun thenTheHttpPortIsCorrect() {
		assertThat(serverInfo!!.httpPort).isEqualTo(52199)
	}

	@Test
	fun thenTheHttpsPortIsCorrect() {
		assertThat(serverInfo!!.httpsPort).isEqualTo(52200)
	}

	@Test
	fun thenTheCertificateFingerprintIsCorrect() {
		assertThat(serverInfo!!.certificateFingerprint)
			.isEqualToIgnoringCase("746E06046B44CED35658F300DB2D08A799DEBC7E")
	}

	@Test
	fun thenTheMacAddressesAreCorrect() {
		assertThat(serverInfo!!.macAddresses).containsExactlyInAnyOrder(
			"5c-f3-70-8b-db-e9",
			"16-15-f4-b9-cd-15",
			"b4-2e-99-31-f7-eb"
		)
	}
}
