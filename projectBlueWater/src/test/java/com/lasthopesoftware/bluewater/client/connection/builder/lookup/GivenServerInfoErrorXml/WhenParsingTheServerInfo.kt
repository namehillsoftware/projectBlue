package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenServerInfoErrorXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerDiscoveryException
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import xmlwise.Xmlwise
import java.util.concurrent.ExecutionException

class WhenParsingTheServerInfo {

	companion object {
		private var exception: ServerDiscoveryException? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val serverInfoXml = mockk<RequestServerInfoXml>()
			every { serverInfoXml.promiseServerInfoXml(any()) } returns	Promise(
						Xmlwise.createXml(
							"""<?xml version="1.0" encoding="UTF-8"?>
<Response Status="Error">
<msg>Keyid gooPc not found.</msg></Response>"""
						)
					)

			val serverLookup = ServerLookup(serverInfoXml)
			try {
				serverLookup.promiseServerInformation(LibraryId(14)).toFuture().get()
			} catch (e: ExecutionException) {
				exception = e.cause as? ServerDiscoveryException ?: throw e
			}
		}
	}

	@Test
	fun thenAServerDiscoveryExceptionIsThrownWithTheCorrectMessage() {
		assertThat(exception?.message).contains("Keyid gooPc not found.")
	}
}
