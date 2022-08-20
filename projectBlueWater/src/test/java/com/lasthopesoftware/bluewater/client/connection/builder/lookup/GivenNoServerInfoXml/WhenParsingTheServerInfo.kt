package com.lasthopesoftware.bluewater.client.connection.builder.lookup.GivenNoServerInfoXml

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.RequestServerInfoXml
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerLookup
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenParsingTheServerInfo {

	private val serverInfo by lazy {
		val serverInfoXml = mockk<RequestServerInfoXml>()
		every { serverInfoXml.promiseServerInfoXml(any()) } returns Promise.empty()
		val serverLookup = ServerLookup(serverInfoXml)
		serverLookup.promiseServerInformation(LibraryId(14)).toExpiringFuture().get()
	}

	@Test
	fun `then no server info is returned`() = assertThat(serverInfo).isNull()
}
