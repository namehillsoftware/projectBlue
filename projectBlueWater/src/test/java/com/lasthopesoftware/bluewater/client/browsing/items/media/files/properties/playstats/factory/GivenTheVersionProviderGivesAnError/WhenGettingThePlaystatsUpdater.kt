package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.GivenTheVersionProviderGivesAnError

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenGettingThePlaystatsUpdater {

	companion object {
		private var exception: ExecutionException? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakeConnectionProvider = FakeConnectionProvider()
			val programVersionProvider = mockk<IProgramVersionProvider>()
			every { programVersionProvider.promiseServerVersion() } returns Promise(Exception(":("))

			val fakeFilePropertiesContainer = FakeFilePropertiesContainer()
			val fakeScopedRevisionProvider = FakeScopedRevisionProvider(20)
			val playstatsUpdateSelector = PlaystatsUpdateSelector(
				fakeConnectionProvider,
				ScopedFilePropertiesProvider(fakeConnectionProvider, fakeScopedRevisionProvider, fakeFilePropertiesContainer),
				ScopedFilePropertiesStorage(fakeConnectionProvider, fakeScopedRevisionProvider, fakeFilePropertiesContainer),
				programVersionProvider
			)
			try {
				playstatsUpdateSelector.promisePlaystatsUpdater().toFuture().get()
			} catch (e: ExecutionException) {
				exception = e
			}
		}
	}

	@Test
	fun thenTheExceptionIsThrown() {
		assertThat(exception).isNotNull
	}
}
