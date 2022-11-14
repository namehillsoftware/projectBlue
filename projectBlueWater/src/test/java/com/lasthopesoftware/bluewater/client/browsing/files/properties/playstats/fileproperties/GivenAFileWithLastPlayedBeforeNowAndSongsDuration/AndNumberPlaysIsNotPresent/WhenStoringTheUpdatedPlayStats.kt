package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsNotPresent

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FakeFilePropertiesContainerRepository
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionConnectionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfConnectionIsReadOnly
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.ProgressingPromise
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

private const val libraryId = 548

class WhenStoringTheUpdatedPlayStats {

	private val services by lazy {

		val libraryConnectionProvider = mockk<ProvideLibraryConnections> {
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

			every { promiseLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(connectionProvider)
		}

		val checkConnection = mockk<CheckIfConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
		val filePropertiesContainer = FakeFilePropertiesContainerRepository()
		val scopedRevisionProvider = LibraryRevisionProvider(libraryConnectionProvider)
		val sessionFilePropertiesProvider =
			FilePropertiesProvider(libraryConnectionProvider, scopedRevisionProvider, filePropertiesContainer)
		Pair(
			FilePropertiesPlayStatsUpdater(
				sessionFilePropertiesProvider,
				FilePropertyStorage(
					libraryConnectionProvider,
					checkConnection,
					scopedRevisionProvider,
					filePropertiesContainer,
					RecordingApplicationMessageBus(),
				)
			),
			sessionFilePropertiesProvider,
		)
	}

	private var fileProperties: Map<String, String>? = null

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, sessionFilePropertiesProvider) = services
		val serviceFile = ServiceFile(23)
		fileProperties = filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(LibraryId(libraryId), serviceFile)
			.eventually {
				sessionFilePropertiesProvider.promiseFileProperties(LibraryId(libraryId), serviceFile)
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
