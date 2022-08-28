package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.GivenTheVersionProviderGivesAnError

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenGettingThePlaystatsUpdater {

	private val playstatsUpdateSelector by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		val programVersionProvider = mockk<IProgramVersionProvider>()
		every { programVersionProvider.promiseServerVersion() } returns Promise(Exception(":("))

		val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly() } returns false.toPromise()

		val fakeFilePropertiesContainer = FakeFilePropertiesContainer()
		val fakeScopedRevisionProvider = FakeScopedRevisionProvider(20)

		PlaystatsUpdateSelector(
			fakeConnectionProvider,
			ScopedFilePropertiesProvider(fakeConnectionProvider, fakeScopedRevisionProvider, fakeFilePropertiesContainer),
			ScopedFilePropertiesStorage(fakeConnectionProvider, checkConnection, fakeScopedRevisionProvider, fakeFilePropertiesContainer),
			programVersionProvider
		)
	}

	private var exception: ExecutionException? = null

	@BeforeAll
	fun act() {
		try {
			playstatsUpdateSelector.promisePlaystatsUpdater().toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e
		}
	}

	@Test
	fun thenTheExceptionIsThrown() {
		assertThat(exception).isNotNull
	}
}
