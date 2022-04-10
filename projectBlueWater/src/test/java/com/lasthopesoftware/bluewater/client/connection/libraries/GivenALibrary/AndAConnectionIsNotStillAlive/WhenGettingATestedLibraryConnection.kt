package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndAConnectionIsNotStillAlive

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.testing.TestConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenGettingATestedLibraryConnection {

	companion object {
		private val statuses = ArrayList<BuildingConnectionStatus>()
		private val expectedConnectionProvider = mockk<IConnectionProvider>()
		private var connectionProvider: IConnectionProvider? = null
		private var secondConnectionProvider: IConnectionProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val testConnections = mockk<TestConnections>()
			every { testConnections.promiseIsConnectionPossible(any()) } returns false.toPromise()

			val firstDeferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
			val secondDeferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

			val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
			every {
				libraryConnectionProvider.promiseLibraryConnection(LibraryId(2))
			} returns firstDeferredConnectionProvider andThen secondDeferredConnectionProvider

			val connectionSessionManager = ConnectionSessionManager(
				testConnections,
				libraryConnectionProvider,
				PromisedConnectionsRepository())

			val libraryId = LibraryId(2)
			val futureConnectionProvider =
				connectionSessionManager
					.promiseLibraryConnection(libraryId)
					.apply {
						progress.then { if (it != null) statuses.add(it) }
						updates(statuses::add)
					}
					.toExpiringFuture()

			firstDeferredConnectionProvider.apply {
				sendProgressUpdates(
					BuildingConnectionStatus.GettingLibrary,
					BuildingConnectionStatus.BuildingConnection,
					BuildingConnectionStatus.BuildingConnectionComplete
				)

				sendResolution(mockk())
			}

			val secondFutureConnectionProvider =
				connectionSessionManager
					.promiseTestedLibraryConnection(libraryId)
					.apply {
						progress.then { if (it != null) statuses.add(it) }
						updates(statuses::add)
					}
					.toExpiringFuture()

			secondDeferredConnectionProvider.apply {
				sendProgressUpdates(
					BuildingConnectionStatus.GettingLibrary,
					BuildingConnectionStatus.BuildingConnection,
					BuildingConnectionStatus.BuildingConnectionComplete
				)

				sendResolution(expectedConnectionProvider)
			}

			connectionProvider = futureConnectionProvider.get()
			secondConnectionProvider = secondFutureConnectionProvider.get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(secondConnectionProvider).isEqualTo(expectedConnectionProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete,
				BuildingConnectionStatus.BuildingConnectionComplete,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}
}
