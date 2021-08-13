package com.lasthopesoftware.bluewater.client.connection.authentication.GivenAReadWriteServer

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenCheckingAuthentication {

	companion object setup {

		private var isReadOnly: Boolean? = false

		@JvmStatic
		@BeforeClass
		fun before() {
			val fakeConnectionProvider = FakeConnectionProvider()
			fakeConnectionProvider.mapResponse({
				FakeConnectionResponseTuple(
					200, (
"""<Response Status="OK">
	<Item Name="Token">B9yXQtTL</Item>
	<Item Name="ReadOnly">0</Item>
	<Item Name="PreLicensed">0</Item>
</Response>""").toByteArray()
				)
			}, "Authenticate")

			val authenticationChecker = ScopedConnectionAuthenticationChecker(fakeConnectionProvider)
			isReadOnly = authenticationChecker.promiseIsReadOnly().toFuture().get()
		}
	}

	@Test
	fun thenTheConnectionIsNotReadOnly() {
		assertThat(isReadOnly).isFalse
	}
}
