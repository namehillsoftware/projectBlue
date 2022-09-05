package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.GivenTheVersionProviderGivesAnError

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdateScopedPlaystats
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.ScopedPlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider
import com.lasthopesoftware.bluewater.client.servers.version.SemanticVersion
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class WhenAttemptingToGetThePlaystatsUpdaterAgain {

	private val playstatsUpdateSelector by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		val programVersionProvider = mockk<IProgramVersionProvider>()
		every { programVersionProvider.promiseServerVersion() } returns Promise(Exception(":(")) andThen Promise(SemanticVersion(22, 0, 0))

		val scopedRevisions = FakeScopedRevisionProvider(1)

		val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly() } returns true.toPromise()

		val fakeFilePropertiesContainer = FakeFilePropertiesContainer()

		PlaystatsUpdateSelector(
			fakeConnectionProvider,
			ScopedFilePropertiesProvider(fakeConnectionProvider, scopedRevisions, fakeFilePropertiesContainer),
			ScopedFilePropertiesStorage(fakeConnectionProvider, checkConnection, scopedRevisions, fakeFilePropertiesContainer),
			programVersionProvider
		)
	}

	private var exception: ExecutionException? = null
	private var updater: UpdateScopedPlaystats? = null

	@BeforeAll
	fun act() {
		try {
			playstatsUpdateSelector.promisePlaystatsUpdater().toExpiringFuture().get()
		} catch (e: ExecutionException) {
			exception = e
		}
		updater = playstatsUpdateSelector.promisePlaystatsUpdater().toExpiringFuture().get()
	}

	@Test
	fun thenTheExceptionIsThrown() {
		assertThat(exception).isNotNull
	}

	@Test
	fun thenThePlayedFilePlaystatsUpdaterIsGiven() {
		assertThat(updater).isInstanceOf(
			ScopedPlayedFilePlayStatsUpdater::class.java
		)
	}
}
