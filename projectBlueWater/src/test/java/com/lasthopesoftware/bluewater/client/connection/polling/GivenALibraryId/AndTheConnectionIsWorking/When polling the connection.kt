package com.lasthopesoftware.bluewater.client.connection.polling.GivenALibraryId.AndTheConnectionIsWorking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPoller
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 242

class `When polling the connection` {

	private val mut by lazy {
		LibraryConnectionPoller(
			mockk {
				every { promiseTestedLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(FakeConnectionProvider())
			}
		)
	}

	private var connectionProvider: IConnectionProvider? = null

	@BeforeAll
	fun act() {
		connectionProvider = mut.pollConnection(LibraryId(libraryId)).toExpiringFuture().get()
	}

	@Test fun `then the connection provider is returned`() {
		assertThat(connectionProvider).isNotNull
	}
}
