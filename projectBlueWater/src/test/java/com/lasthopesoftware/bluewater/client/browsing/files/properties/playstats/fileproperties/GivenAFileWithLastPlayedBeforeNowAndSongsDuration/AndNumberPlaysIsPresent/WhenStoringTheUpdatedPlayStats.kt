package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsPresent

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainer
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
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenStoringTheUpdatedPlayStats {

	private val services by lazy {
		val connectionProvider = FakeRevisionConnectionProvider()
		connectionProvider.setSyncRevision(1)

		val duration = Duration.standardMinutes(5).millis
		originalLastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds

		connectionProvider.mapResponse(
			{
				FakeConnectionResponseTuple(
					200, """<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<MPL Version="2.0" Title="MCWS - Files - 10936" PathSeparator="\">
<Item>
<Field Name="Key">23</Field>
<Field Name="Media Type">Audio</Field>
<Field Name="${KnownFileProperties.LAST_PLAYED}">$originalLastPlayed</Field>
<Field Name="Rating">4</Field>
<Field Name="File Size">2345088</Field>
<Field Name="${KnownFileProperties.DURATION}">$duration</Field>
<Field Name="${KnownFileProperties.NUMBER_PLAYS}">52</Field>
</Item>
</MPL>
""".toByteArray()
				)
			},
			"File/GetInfo", "File=23"
		)

		val checkScopedRevision = ScopedRevisionProvider(connectionProvider)
		val filePropertiesContainer = FakeFilePropertiesContainer()
		val sessionFilePropertiesProvider =
			ScopedFilePropertiesProvider(connectionProvider, checkScopedRevision, filePropertiesContainer)
		val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly() } returns false.toPromise()
		val scopedFilePropertiesPlayStatsUpdater = ScopedFilePropertiesPlayStatsUpdater(
			sessionFilePropertiesProvider,
			ScopedFilePropertiesStorage(
				connectionProvider,
				checkConnection,
				checkScopedRevision,
				filePropertiesContainer,
				RecordingApplicationMessageBus()
			)
		)
		Pair(scopedFilePropertiesPlayStatsUpdater, sessionFilePropertiesProvider)
	}

	private var fileProperties: Map<String, String>? = null
	private var originalLastPlayed: Long = 0

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, sessionFilePropertiesProvider) = services
		fileProperties = filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(ServiceFile(23))
			.eventually { sessionFilePropertiesProvider.promiseFileProperties(ServiceFile(23)) }
			.toExpiringFuture()
			.get()
	}

	@Test
	fun thenTheLastPlayedIsRecent() {
		assertThat(fileProperties!![KnownFileProperties.LAST_PLAYED]?.toLong()).isGreaterThan(originalLastPlayed)
	}

	@Test
	fun thenTheNumberPlaysIsIncremented() {
		assertThat(fileProperties!![KnownFileProperties.NUMBER_PLAYS]).isEqualTo("53")
	}
}
