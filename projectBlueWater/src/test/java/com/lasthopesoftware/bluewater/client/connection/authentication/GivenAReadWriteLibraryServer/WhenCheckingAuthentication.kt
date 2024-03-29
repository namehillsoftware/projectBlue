package com.lasthopesoftware.bluewater.client.connection.authentication.GivenAReadWriteLibraryServer

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val libraryId = 652

class WhenCheckingAuthentication {

	private val isReadOnly by lazy {
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

		val libraryConnectionProvider = mockk<ProvideLibraryConnections> {
			every { promiseLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(fakeConnectionProvider)
		}

		val authenticationChecker = ConnectionAuthenticationChecker(libraryConnectionProvider)
		authenticationChecker.promiseIsReadOnly(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test
	fun `then the connection is not read only`() {
		assertThat(isReadOnly).isFalse
	}
}
