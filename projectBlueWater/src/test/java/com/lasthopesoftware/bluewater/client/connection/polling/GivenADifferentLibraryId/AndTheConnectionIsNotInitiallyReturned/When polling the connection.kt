package com.lasthopesoftware.bluewater.client.connection.polling.GivenADifferentLibraryId.AndTheConnectionIsNotInitiallyReturned

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
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
import java.util.concurrent.TimeUnit
import kotlin.math.log2

private const val libraryId = 79

class `When polling the connection` {

	private val mut by lazy {
        LibraryConnectionPoller(
            mockk {
				val nullIterations = log2(32.0).toInt()
				val connectionResponses = MutableList(nullIterations) { ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>(null as IConnectionProvider?) }
				connectionResponses.add(ProgressingPromise(FakeConnectionProvider()))
                every { promiseTestedLibraryConnection(LibraryId(libraryId)) } returnsMany connectionResponses
            }
        )
	}

	private var connectionProvider: IConnectionProvider? = null

	@BeforeAll
	fun act() {
		connectionProvider = mut.pollConnection(LibraryId(libraryId)).toExpiringFuture().get(3, TimeUnit.MINUTES)
	}

	@Test
    fun `then the connection provider is returned`() {
		assertThat(connectionProvider).isNotNull
	}
}
