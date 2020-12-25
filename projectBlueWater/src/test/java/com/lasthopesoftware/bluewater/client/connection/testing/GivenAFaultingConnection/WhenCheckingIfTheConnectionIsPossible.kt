package com.lasthopesoftware.bluewater.client.connection.testing.GivenAFaultingConnection

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class WhenCheckingIfTheConnectionIsPossible {
	@Test
	fun thenTheResultIsCorrect() {
		AssertionsForClassTypes.assertThat(result).isFalse
	}

	companion object {
		private var result = false

		@BeforeClass
		@JvmStatic
		@Throws(ExecutionException::class, InterruptedException::class)
		fun before() {
			val connectionTester = ConnectionTester()
			val connectionProvider = FakeConnectionProvider()
			connectionProvider.mapResponse({ throw IOException() }, "Alive")
			result = FuturePromise(connectionTester.promiseIsConnectionPossible(connectionProvider)).get()!!
		}
	}
}
