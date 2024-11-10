package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenServerInfoErrorXml.AndLocalServerSettings

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 592

class `When Getting the Server Info` {
	private val services by lazy {
        ServerLookup(
            mockk {
                every { lookupConnectionSettings(LibraryId(libraryId)) } returns ConnectionSettings(
                    accessCode = "acziGulWke",
                    macAddress = "",
                ).toPromise()
            },
			mockk {
				every { promiseServerInfoXml(any()) } returns Promise(
					Jsoup.parse(
						"""<?xml version="1.0" encoding="UTF-8"?>
<Response Status="Error">
<msg>Tristiquesem adipiscingcursus diamnibh.</msg></Response>""",
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
		assertThat(serverInfo).isEqualTo(ServerInfo(
			remoteHost = "acziGulWke",
			httpPort = 80,
			macAddresses = emptySet()
		))
	}
}
