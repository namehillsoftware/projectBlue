package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class WhenGettingATestedLibraryConnection {

	companion object {
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private var connectionProvider: IConnectionProvider? = null
		private var secondConnectionProvider: IConnectionProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionsTester = mockk<TestConnections>()
			every { connectionsTester.promiseIsConnectionPossible(any()) } returns true.toPromise()

			val firstDeferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
			val secondDeferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

			val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
			every {
				libraryConnectionProvider.promiseLibraryConnection(LibraryId(2))
			} returns firstDeferredConnectionProvider andThen secondDeferredConnectionProvider

			val connectionSessionManager = ConnectionSessionManager(
				connectionsTester,
				libraryConnectionProvider
			)

			val libraryId = LibraryId(2)

			val futureConnectionProvider =
				connectionSessionManager
					.promiseLibraryConnection(libraryId)
					.apply { updates(statuses::add) }
					.toFuture()

			firstDeferredConnectionProvider.apply {
				sendProgressUpdates(
					BuildingConnectionStatus.GettingLibrary,
					BuildingConnectionStatus.BuildingConnection,
					BuildingConnectionStatus.BuildingConnectionComplete
				)

				sendResolution(mockk())
			}

			connectionProvider = futureConnectionProvider[30, TimeUnit.SECONDS]

			val secondFutureConnectionProvider =
				connectionSessionManager
					.promiseTestedLibraryConnection(libraryId)
					.apply { updates(statuses::add) }
					.toFuture()

			secondDeferredConnectionProvider.apply {
				sendProgressUpdates(
					BuildingConnectionStatus.GettingLibrary,
					BuildingConnectionStatus.BuildingConnection,
					BuildingConnectionStatus.BuildingConnectionComplete
				)

				sendResolution(mockk())
			}

			secondConnectionProvider = secondFutureConnectionProvider.get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider).isEqualTo(connectionProvider!!)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}
}
