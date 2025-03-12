package com.lasthopesoftware.bluewater.client.access.jriver.GivenAJRiverConnection.AndItDoesNotHaveAProgramVersion

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.FakeJRiverConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenReceivingThePromisedProgramVersion {
    private val version by lazy {
		val connectionProvider = FakeJRiverConnectionProvider()
		connectionProvider.mapResponse({
			FakeConnectionResponseTuple(
				200,
				"<Response Status=\"OK\"></Response>".toByteArray()
			)
		}, "Alive")
		val programVersionProvider = JRiverLibraryAccess(connectionProvider)
		programVersionProvider.promiseServerVersion().toExpiringFuture().get()
	}

    @Test
    fun `then the server version is null`() {
        assertThat(version).isNull()
    }
}
