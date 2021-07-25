package com.lasthopesoftware.bluewater.client.connection.libraries.GivenALibrary

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.settings.ValidateConnectionSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenRetrievingTheLibraryConnection {

	companion object {
		private var expectedConnectionProvider = mockk<IConnectionProvider>()
		private var connectionProvider: IConnectionProvider? = null
		private var isActiveBeforeGettingConnection = false
		private var isActiveAfterGettingConnection = false

		@BeforeClass
		@JvmStatic
		fun before() {
			val validateConnectionSettings = mockk<ValidateConnectionSettings>()
			every { validateConnectionSettings.isValid(any()) } returns true

			val deferredConnectionProvider = DeferredProgressingPromise<BuildingConnectionStatus, IConnectionProvider?>()

			val libraryConnectionProvider = mockk<ProvideLibraryConnections>()
			every { libraryConnectionProvider.promiseLibraryConnection(LibraryId(3)) } returns deferredConnectionProvider

			val connectionSessionManager = ConnectionSessionManager(
				mockk(),
				libraryConnectionProvider,
			)

			val futureConnectionProvider =
				connectionSessionManager
					.promiseLibraryConnection(LibraryId(3))
					.toFuture()

			isActiveBeforeGettingConnection = connectionSessionManager.isConnectionActive(LibraryId(3))
			deferredConnectionProvider.sendResolution(expectedConnectionProvider)
			connectionProvider = futureConnectionProvider.get()
			isActiveAfterGettingConnection = connectionSessionManager.isConnectionActive(LibraryId(3))
		}
	}

	@Test
	fun thenTheConnectionIsNotActiveBeforeGettingConnection() {
		assertThat(isActiveBeforeGettingConnection).isFalse
	}

	@Test
	fun thenTheConnectionIsCorrect() {
		assertThat(connectionProvider).isEqualTo(expectedConnectionProvider)
	}

	@Test
	fun thenTheConnectionIsActiveAfterGettingConnection() {
		assertThat(isActiveAfterGettingConnection).isTrue
	}
}
