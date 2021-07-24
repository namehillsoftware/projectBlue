package com.lasthopesoftware.bluewater.client.connection.session.GivenANullLibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenTestingIfTheConnectionIsActive {

	companion object {
		private var isActive = false

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionSessionManager = ConnectionSessionManager(mockk(), mockk())

			isActive = connectionSessionManager.isConnectionActive(LibraryId(2))
		}
	}

	@Test
	fun thenTheConnectionIsNotActive() {
		assertThat(isActive).isFalse
	}
}
