package com.lasthopesoftware.bluewater.client.connection.lookup.GivenServerInfoXml.AndAnAccessCodeThatHasAPort

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
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 495

class `When Getting the Server Info` {
	private val services by lazy {
        ServerLookup(
            mockk {
                every { lookupConnectionSettings(LibraryId(libraryId)) } returns MediaCenterConnectionSettings(
                    accessCode = "gyRxp8AOEkD:302",
					sslCertificateFingerprint = Hex.decodeHex("d0a3e4bf62221422a2e5dc9c479c2b36"),
                ).toPromise()
            },
            mockk {
                every { promiseServerInfoXml(LibraryId(libraryId)) } returns Promise(
					Jsoup.parse(
						"""<?xml version="1.0" encoding="UTF-8"?>
<Response Status="OK">
<keyid>gooPc</keyid>
<ip>108.491.23.154</ip>
<port>52199</port>
<localiplist>169.254.72.216,192.940.1.817</localiplist>
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
            },
        )
	}

	private var serverInfo: ServerInfo? = null

	@BeforeAll
	fun act() {
		serverInfo = services.promiseServerInformation(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the server info is correct`() {
        assertThat(serverInfo).isEqualTo(
            ServerInfo(
                httpPort = null,
                httpsPort = 302,
                remoteHost = "gyRxp8AOEkD",
                macAddresses = emptySet(),
                localIps = emptySet(),
                certificateFingerprint = Hex.decodeHex("d0a3e4bf62221422a2e5dc9c479c2b36"),
            )
        )
	}
}
