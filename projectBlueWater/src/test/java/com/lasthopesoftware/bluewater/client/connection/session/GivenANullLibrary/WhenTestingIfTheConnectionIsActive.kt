package com.lasthopesoftware.bluewater.client.connection.session.GivenANullLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenTestingIfTheConnectionIsActive {

	private val mut by lazy {
		ConnectionSessionManager(mockk(), mockk(), PromisedConnectionsRepository())
	}

	private var isActive = false

	@BeforeAll
	fun act() {
		isActive = mut.isConnectionActive(LibraryId(2))
	}

	@Test
	fun thenTheConnectionIsNotActive() {
		assertThat(isActive).isFalse
	}
}
