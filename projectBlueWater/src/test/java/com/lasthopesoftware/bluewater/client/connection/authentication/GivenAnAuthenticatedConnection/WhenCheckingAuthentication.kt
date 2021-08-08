package com.lasthopesoftware.bluewater.client.connection.authentication.GivenAnAuthenticatedConnection

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenCheckingAuthentication {

	companion object setup {

		private var isAuthenticated: Boolean? = false

		@JvmStatic
		@BeforeClass
		fun before() {
			val selectedConnections = mockk<ProvideSelectedConnection>()
			every { selectedConnections.promiseSessionConnection() } returns Promise(FakeConnectionProvider())

			val authenticationChecker = SelectedConnectionAuthenticationChecker(selectedConnections)
			isAuthenticated = authenticationChecker.promiseIsAuthenticated().toFuture().get()
		}
	}

	@Test
	fun thenTheConnectionReportsAuthenticated() {
		assertThat(isAuthenticated).isTrue
	}
}
