package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsPresent

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
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
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
			originalLastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds

			connectionProvider.mapResponse(
				{
					FakeConnectionResponseTuple(
						200, """<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<MPL Version="2.0" Title="MCWS - Files - 10936" PathSeparator="\">
<Item>
<Field Name="Key">23</Field>
<Field Name="Media Type">Audio</Field>
<Field Name="${KnownFileProperties.LastPlayed}">$originalLastPlayed</Field>
<Field Name="Rating">4</Field>
<Field Name="File Size">2345088</Field>
<Field Name="${KnownFileProperties.Duration}">$duration</Field>
<Field Name="${KnownFileProperties.NumberPlays}">52</Field>
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
		val revisionProvider = LibraryRevisionProvider(libraryConnectionProvider)
		val filePropertiesProvider = FilePropertiesProvider(
			GuaranteedLibraryConnectionProvider(libraryConnectionProvider),
			revisionProvider,
			filePropertiesContainer
		)
		Pair(
			FilePropertiesPlayStatsUpdater(
				filePropertiesProvider,
				FilePropertyStorage(
					libraryConnectionProvider,
					checkConnection,
					revisionProvider,
					filePropertiesContainer,
					RecordingApplicationMessageBus(),
				)
			),
			filePropertiesProvider,
		)
	}

	private var fileProperties: Map<String, String>? = null
	private var originalLastPlayed: Long = 0

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, sessionFilePropertiesProvider) = services
		fileProperties = filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile(23))
			.eventually { sessionFilePropertiesProvider.promiseFileProperties(LibraryId(libraryId), ServiceFile(23)) }
			.toExpiringFuture()
			.get()
	}

	@Test
	fun thenTheLastPlayedIsRecent() {
		assertThat(fileProperties!![KnownFileProperties.LastPlayed]?.toLong()).isGreaterThan(originalLastPlayed)
	}

	@Test
	fun thenTheNumberPlaysIsIncremented() {
		assertThat(fileProperties!![KnownFileProperties.NumberPlays]).isEqualTo("53")
	}
}
