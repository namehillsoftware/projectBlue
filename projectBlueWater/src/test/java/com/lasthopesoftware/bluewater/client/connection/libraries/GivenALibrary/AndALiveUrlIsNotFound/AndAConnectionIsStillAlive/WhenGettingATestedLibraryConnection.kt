package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary.AndALiveUrlIsNotFound.AndAConnectionIsStillAlive

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

class WhenGettingATestedLibraryConnection {
	companion object {
		private val statuses: MutableList<BuildingConnectionStatus> = ArrayList()
		private val expectedConnectionProvider = mockk<IConnectionProvider>()
		private var connectionProvider: IConnectionProvider? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val connectionsTester = mockk<TestConnections>()
			every  { connectionsTester.promiseIsConnectionPossible(any()) } returns false.toPromise()

			val firstDeferredLibraryConnection = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()
			val secondDeferredLibraryConnection = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

			val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
			every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(2)) } returns firstDeferredLibraryConnection andThen secondDeferredLibraryConnection

			val connectionSessionManager = ConnectionSessionManager(
				connectionsTester,
				libraryConnectionProvider
			)
			val libraryId = LibraryId(2)
			val futureConnectionProvider = connectionSessionManager
				.promiseLibraryConnection(libraryId)
				.updates(statuses::add)
				.eventually(
					{
						connectionSessionManager.promiseTestedLibraryConnection(libraryId)
							.updates(statuses::add)
					},
					{
						connectionSessionManager.promiseTestedLibraryConnection(libraryId)
							.updates(statuses::add)
					})
				.toFuture()

			firstDeferredLibraryConnection.apply {
				sendProgressUpdates(
					BuildingConnectionStatus.GettingLibrary,
					BuildingConnectionStatus.BuildingConnection,
					BuildingConnectionStatus.BuildingConnectionFailed,
				)

				sendResolution(null)
			}

			secondDeferredLibraryConnection.apply {
				sendProgressUpdates(
					BuildingConnectionStatus.GettingLibrary,
					BuildingConnectionStatus.BuildingConnection,
					BuildingConnectionStatus.BuildingConnectionComplete
				)

				sendResolution(expectedConnectionProvider)
			}
			connectionProvider = futureConnectionProvider.get()
		}
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider).isEqualTo(expectedConnectionProvider)
	}

	@Test
	fun thenGettingLibraryIsBroadcast() {
		assertThat(statuses)
			.containsExactly(
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionFailed,
				BuildingConnectionStatus.GettingLibrary,
				BuildingConnectionStatus.BuildingConnection,
				BuildingConnectionStatus.BuildingConnectionComplete
			)
	}
}
