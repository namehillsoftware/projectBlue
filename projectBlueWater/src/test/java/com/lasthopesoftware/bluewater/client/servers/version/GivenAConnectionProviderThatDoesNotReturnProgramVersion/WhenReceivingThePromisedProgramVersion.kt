package com.lasthopesoftware.bluewater.client.servers.version.GivenAConnectionProviderThatDoesNotReturnProgramVersion

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.servers.version.LibraryServerVersionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenReceivingThePromisedProgramVersion {
    private val version by lazy {
		val connectionProvider = FakeConnectionProvider()
		connectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				"<Response Status=\"OK\"></Response>".toByteArray()
			)
		}, "Alive")
		val programVersionProvider = LibraryServerVersionProvider(mockk {
			every { promiseLibraryConnection(LibraryId(934)) } returns ProgressingPromise(connectionProvider)
		})
		programVersionProvider.promiseServerVersion(LibraryId(934)).toExpiringFuture().get()
	}

    @Test
    fun `then the server version is null`() {
        assertThat(version).isNull()
    }
}
