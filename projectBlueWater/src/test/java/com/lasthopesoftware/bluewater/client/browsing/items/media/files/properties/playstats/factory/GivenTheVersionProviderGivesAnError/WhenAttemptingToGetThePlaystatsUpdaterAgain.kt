package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.GivenTheVersionProviderGivesAnError

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.IPlaystatsUpdate
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ExecutionException

class WhenAttemptingToGetThePlaystatsUpdaterAgain {

	companion object {
		private var exception: ExecutionException? = null
		private var updater: IPlaystatsUpdate? = null

		@BeforeClass
		@JvmStatic
		fun before() {
			val fakeConnectionProvider = FakeConnectionProvider()
			val programVersionProvider = mockk<IProgramVersionProvider>()
			every { programVersionProvider.promiseServerVersion() } returns Promise(Exception(":(")) andThen Promise(SemanticVersion(22, 0, 0))

			val scopedRevisions = FakeScopedRevisionProvider(1)

			val fakeFilePropertiesContainer = FakeFilePropertiesContainer()
			val playstatsUpdateSelector = PlaystatsUpdateSelector(
				fakeConnectionProvider,
				ScopedFilePropertiesProvider(fakeConnectionProvider, scopedRevisions, fakeFilePropertiesContainer),
				ScopedFilePropertiesStorage(fakeConnectionProvider, scopedRevisions, fakeFilePropertiesContainer),
				programVersionProvider
			)
			try {
				playstatsUpdateSelector.promisePlaystatsUpdater().toFuture().get()
			} catch (e: ExecutionException) {
				exception = e
			}
			updater = playstatsUpdateSelector.promisePlaystatsUpdater().toFuture().get()
		}
	}

	@Test
	fun thenTheExceptionIsThrown() {
		Assertions.assertThat(exception).isNotNull
	}

	@Test
	fun thenThePlayedFilePlaystatsUpdaterIsGiven() {
		Assertions.assertThat(updater).isInstanceOf(
			PlayedFilePlayStatsUpdater::class.java
		)
	}
}
