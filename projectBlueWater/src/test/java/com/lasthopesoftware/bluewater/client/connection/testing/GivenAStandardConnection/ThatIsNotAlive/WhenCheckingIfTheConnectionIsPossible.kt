package com.lasthopesoftware.bluewater.client.connection.testing.GivenAStandardConnection.ThatIsNotAlive

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test

class WhenCheckingIfTheConnectionIsPossible {

	private val result by lazy {
		val connectionTester = com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
		val connectionProvider = FakeConnectionProvider()
		connectionProvider.mapResponse(
			{
				FakeConnectionResponseTuple(
					200,
					(
						"<Response Status=\"NOT-OK\">" +
							"<Item Name=\"Master\">1192</Item>" +
							"<Item Name=\"Sync\">1192</Item>" +
							"<Item Name=\"LibraryStartup\">1501430846</Item>" +
							"</Response>"
						).toByteArray()
				)
			}, "Alive"
		)
		connectionTester.promiseIsConnectionPossible(connectionProvider).toExpiringFuture().get()!!
	}

	@Test
	fun `then the result is correct`() {
		assertThat(result).isFalse
	}
}
