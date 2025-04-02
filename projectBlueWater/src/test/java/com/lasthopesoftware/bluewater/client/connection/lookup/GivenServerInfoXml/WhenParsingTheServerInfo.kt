package com.lasthopesoftware.bluewater.client.connection.lookup.GivenServerInfoXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.RequestServerInfoXml
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.binary.Hex
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.jupiter.api.Test

class WhenParsingTheServerInfo {

	private val serverInfo by lazy {
		val serverInfoXml = mockk<RequestServerInfoXml>()
		every { serverInfoXml.promiseServerInfoXml(any()) } returns Promise(
			Jsoup.parse(
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
</Response>""",
				"",
				Parser.xmlParser()
			)
		)
		val serverLookup = ServerLookup(
			mockk {
				every { promiseConnectionSettings(any()) } returns Promise.empty()
			},
			serverInfoXml)
		serverLookup.promiseServerInformation(LibraryId(10)).toExpiringFuture().get()
	}

	@Test
	fun `then the remote ip is correct`() {
		assertThat(serverInfo!!.remoteHost).isEqualTo("108.491.23.154")
	}

	@Test
	fun `then the local ips are correct`() {
		assertThat(serverInfo!!.localIps)
			.containsExactlyInAnyOrder("169.254.72.216", "192.168.1.50")
	}

	@Test
	fun `then the http port is correct`() {
		assertThat(serverInfo!!.httpPort).isEqualTo(52199)
	}

	@Test
	fun `then the https port is correct`() {
		assertThat(serverInfo!!.httpsPort).isEqualTo(52200)
	}

	@Test
	fun `then the certificate fingerprint is correct`() {
		assertThat(String(Hex.encodeHex(serverInfo!!.certificateFingerprint)))
			.isEqualToIgnoringCase("746E06046B44CED35658F300DB2D08A799DEBC7E")
	}

	@Test
	fun `then the mac addresses are correct`() {
		assertThat(serverInfo!!.macAddresses).containsExactlyInAnyOrder(
			"5c-f3-70-8b-db-e9",
			"16-15-f4-b9-cd-15",
			"b4-2e-99-31-f7-eb"
		)
	}
}
