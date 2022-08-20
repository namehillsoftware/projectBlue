package com.lasthopesoftware.bluewater.client.connection.testing.GivenAFaultingConnection

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

class WhenCheckingIfTheConnectionIsPossible {

	private val result by lazy {
		val connectionTester = ConnectionTester
		val connectionProvider = FakeConnectionProvider()
		connectionProvider.mapResponse({ throw IOException() }, "Alive")
		connectionTester.promiseIsConnectionPossible(connectionProvider).toExpiringFuture().get()!!
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
