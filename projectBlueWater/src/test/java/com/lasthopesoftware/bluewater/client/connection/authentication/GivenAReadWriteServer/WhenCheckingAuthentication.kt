package com.lasthopesoftware.bluewater.client.connection.authentication.GivenAReadWriteServer

import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.authentication.ScopedConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenCheckingAuthentication {

	private val services by lazy {
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
		authenticationChecker
	}

	private var isReadOnly: Boolean? = false

	@BeforeAll
	fun act() {
		isReadOnly = services.promiseIsReadOnly().toExpiringFuture().get()
	}

	@Test
	fun `then the connection is not read only`() {
		assertThat(isReadOnly).isFalse
	}
}
