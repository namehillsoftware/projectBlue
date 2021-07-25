package com.lasthopesoftware.bluewater.client.connection.testing.GivenAConnectionWithBadData

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenCheckingIfTheConnectionIsPossible {

	companion object {
		private var result = false

		@JvmStatic
		@BeforeClass
		fun before() {
			val connectionTester = ConnectionTester
			val connectionProvider = FakeConnectionProvider()
			connectionProvider.mapResponse(
				{
					FakeConnectionResponseTuple(
						200,
						(
							"{Response Status=\"NOT-OK\"}}" +
							"<Item Name=\"Master\">1192</Item>" +
							"<Item Name=\"Sync\">1192</Item>" +
							"{{Item Name=\"LibraryStartup\">1501430846</Item>" +
							"</Response>"
						).toByteArray()
					)
				}, "Alive")
			result = connectionTester.promiseIsConnectionPossible(connectionProvider).toFuture().get()!!
		}
	}

	@Test
	fun thenTheResultIsCorrect() {
		assertThat(result).isFalse
	}
}
