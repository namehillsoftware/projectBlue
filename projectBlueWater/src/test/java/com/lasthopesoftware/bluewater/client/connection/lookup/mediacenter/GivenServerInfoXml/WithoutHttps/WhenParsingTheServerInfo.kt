package com.lasthopesoftware.bluewater.client.connection.lookup.mediacenter.GivenServerInfoXml.WithoutHttps

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.lookup.RequestMediaCenterServerInfoXml
import com.lasthopesoftware.bluewater.client.connection.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.jupiter.api.Test

class WhenParsingTheServerInfo {

	private val serverInfo by lazy {
		val serverInfoXml = mockk<RequestMediaCenterServerInfoXml>()
		every { serverInfoXml.promiseServerInfoXml(any()) } returns Promise(
			Jsoup.parse(
				"""<?xml version="1.0" encoding="UTF-8"?>
<Response Status="OK">
<keyid>gooPc</keyid>
<ip>108.491.23.154</ip>
<port>52199</port>
<localiplist>169.254.72.216,192.168.1.50</localiplist>
<macaddresslist>
5c-f3-70-8b-db-e9,b4-2e-99-31-f7-eb
</macaddresslist>
</Response>""",
				"",
				Parser.xmlParser()
			)
		)
		val serverLookup = ServerLookup(
			mockk {
				every { promiseConnectionSettings(LibraryId(10)) } returns MediaCenterConnectionSettings(accessCode = "").toPromise()
			},
			serverInfoXml,
        )
		serverLookup.promiseServerInformation(LibraryId(10)).toExpiringFuture().get()
	}

	@Test
	fun `then the remote ip is correct`() {
		assertThat(serverInfo!!.remoteHosts).isEqualTo(setOf("108.491.23.154"))
	}

	@Test
	fun `then the local ips are correct`() {
		assertThat(serverInfo!!.localHosts).contains("169.254.72.216", "192.168.1.50")
	}

	@Test
	fun `then the http port is correct`() {
		assertThat(serverInfo!!.httpPort).isEqualTo(52199)
	}

	@Test
	fun `then the https port is null`() {
		assertThat(serverInfo!!.httpsPort).isNull()
	}

	@Test
	fun `then the certificate fingerprint is correct is null`() {
		assertThat(serverInfo!!.certificateFingerprint).isEmpty()
	}

	@Test
	fun `then the mac addresses are correct`() {
		assertThat(serverInfo!!.macAddresses)
			.containsExactlyInAnyOrder("5c-f3-70-8b-db-e9", "b4-2e-99-31-f7-eb")
	}
}
