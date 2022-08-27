package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenServerInfoErrorXml.WithoutAMessage

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerDiscoveryException
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xmlwise.Xmlwise
import java.util.concurrent.ExecutionException

class WhenParsingTheServerInfo {

	private val module by lazy {
		val serverInfoXml = mockk<RequestServerInfoXml>()
		every { serverInfoXml.promiseServerInfoXml(any()) } returns Promise(
			Xmlwise.createXml(
				"""<?xml version="1.0" encoding="UTF-8"?>
<Response Status="Error">
</Response>"""
			)
		)

		val serverLookup = ServerLookup(serverInfoXml)
		serverLookup
	}

	private var exception: ServerDiscoveryException? = null

	@BeforeAll
	fun act() {
		try {
			module.promiseServerInformation(LibraryId(5)).toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e.cause as? ServerDiscoveryException ?: throw e
		}
	}

	@Test
	fun `then a server discovery exception is thrown with the correct message`() {
		assertThat(exception).isNotNull
	}
}
