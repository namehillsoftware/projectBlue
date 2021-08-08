package com.lasthopesoftware.bluewater.client.connection.authentication.GivenAnUnauthenticatedConnection

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.SelectedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
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

			val connectionProvider = mockk<IConnectionProvider>()

			val urlProvider = mockk<IUrlProvider>()
			every { urlProvider.authCode } returns null

			every { connectionProvider.urlProvider } returns urlProvider
			every { selectedConnections.promiseSessionConnection() } returns Promise(connectionProvider)

			val authenticationChecker = SelectedConnectionAuthenticationChecker(selectedConnections)
			isAuthenticated = authenticationChecker.isAuthenticated().toFuture().get()
		}
	}

	@Test
	fun thenTheConnectionReportsUnauthenticated() {
		assertThat(isAuthenticated).isFalse
	}
}
