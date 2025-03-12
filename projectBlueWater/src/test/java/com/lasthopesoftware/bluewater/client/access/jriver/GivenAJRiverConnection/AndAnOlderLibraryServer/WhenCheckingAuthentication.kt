package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndAnOlderLibraryServer

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenCheckingAuthentication {

	private val isReadOnly by lazy {
		val fakeConnectionProvider = FakeJRiverConnectionProvider()
		fakeConnectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200, (
					"""<Response Status="OK">
<Item Name="Token">B9yXQtTL</Item>
<Item Name="PreLicensed">0</Item>
</Response>""").toByteArray()
			)
		}, "Authenticate")

		val access = JRiverLibraryAccess(fakeConnectionProvider)
		access.promiseIsReadOnly().toExpiringFuture().get()
	}

	@Test
	fun `then the connection is not read only`() {
		assertThat(isReadOnly).isFalse
	}
}
