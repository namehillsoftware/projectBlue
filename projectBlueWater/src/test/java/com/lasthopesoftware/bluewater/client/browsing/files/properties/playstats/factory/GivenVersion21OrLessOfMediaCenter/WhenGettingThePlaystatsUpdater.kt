package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.GivenVersion21OrLessOfMediaCenter

import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlaystats
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory.PlaystatsUpdateSelector
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
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

class WhenGettingThePlaystatsUpdater {

	private val playstatsUpdateSelector by lazy {
		val fakeConnectionProvider = FakeConnectionProvider()
		val programVersionProvider = mockk<IProgramVersionProvider>()
		every { programVersionProvider.promiseServerVersion() } returns Promise(SemanticVersion(21, 0, 0))
		val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly() } returns false.toPromise()
		val fakeFilePropertiesContainer = FakeFilePropertiesContainer()
		val scopedRevisionProvider = FakeScopedRevisionProvider(10)

		PlaystatsUpdateSelector(
			fakeConnectionProvider,
			ScopedFilePropertiesProvider(fakeConnectionProvider, scopedRevisionProvider, fakeFilePropertiesContainer),
			ScopedFilePropertiesStorage(fakeConnectionProvider, checkConnection, scopedRevisionProvider, fakeFilePropertiesContainer),
			programVersionProvider
		)
	}

	private var updater: UpdatePlaystats? = null

	@BeforeAll
	fun act() {
		updater = playstatsUpdateSelector.promisePlaystatsUpdater().toExpiringFuture().get()
	}

	@Test
	fun thenTheFilePropertiesPlaystatsUpdaterIsGiven() {
		assertThat(updater).isInstanceOf(
			FilePropertiesPlayStatsUpdater::class.java
		)
	}
}
