package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsNotPresent

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.ScopedFilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionConnectionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenStoringTheUpdatedPlayStats {

	private val services by lazy {
		val connectionProvider = FakeRevisionConnectionProvider()
		connectionProvider.setSyncRevision(1)
		val duration = Duration.standardMinutes(5).millis
		val lastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds

		connectionProvider.mapResponse(
			{
				FakeConnectionResponseTuple(
					200, """<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<MPL Version="2.0" Title="MCWS - Files - 10936" PathSeparator="\">
<Item>
<Field Name="Key">23</Field>
<Field Name="Media Type">Audio</Field>
<Field Name="${KnownFileProperties.LastPlayed}">$lastPlayed</Field>
<Field Name="Rating">4</Field>
<Field Name="File Size">2345088</Field>
<Field Name="${KnownFileProperties.Duration}">$duration</Field>
</Item>
</MPL>
""".toByteArray()
				)
			},
			"File/GetInfo", "File=23"
		)

		val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly() } returns false.toPromise()
		val filePropertiesContainer = FakeFilePropertiesContainerRepository()
		val scopedRevisionProvider = ScopedRevisionProvider(connectionProvider)
		val sessionFilePropertiesProvider =
			ScopedFilePropertiesProvider(connectionProvider, scopedRevisionProvider, filePropertiesContainer)
		Pair(ScopedFilePropertiesPlayStatsUpdater(
			sessionFilePropertiesProvider,
			ScopedFilePropertiesStorage(
				connectionProvider,
				checkConnection,
				scopedRevisionProvider,
				filePropertiesContainer,
				RecordingApplicationMessageBus(),
			)
		), sessionFilePropertiesProvider)
	}

	private var fileProperties: Map<String, String>? = null

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, sessionFilePropertiesProvider) = services
		val serviceFile = ServiceFile(23)
		fileProperties = filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(serviceFile)
			.eventually {
				sessionFilePropertiesProvider.promiseFileProperties(serviceFile)
			}
			.toExpiringFuture()
			.get()
	}

	@Test
	fun thenTheLastPlayedIsRecent() {
		assertThat(fileProperties!![KnownFileProperties.LastPlayed]?.toLong()).isCloseTo(Duration.millis(System.currentTimeMillis()).standardSeconds, Offset.offset(10L))
	}

	@Test
	fun thenTheNumberPlaysIsIncremented() {
		assertThat(fileProperties!![KnownFileProperties.NumberPlays]).isEqualTo("1")
	}
}
