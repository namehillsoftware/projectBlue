package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedInTheFuture

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
import com.lasthopesoftware.promises.extensions.ProgressingPromise
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 548
private const val serviceFileId = 894

class WhenStoringTheUpdatedPlayStats {

	private val services by lazy {
		val libraryConnectionProvider = mockk<ProvideLibraryConnections> {
			val connectionProvider = FakeRevisionConnectionProvider()
			connectionProvider.setSyncRevision(1)
			val duration = Duration.standardMinutes(5).millis

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
<Field Name="${KnownFileProperties.NumberPlays}">52</Field>
</Item>
</MPL>
""".toByteArray()
					)
				},
				"File/GetInfo", "File=$serviceFileId"
			)

			every { promiseLibraryConnection(LibraryId(libraryId)) } returns ProgressingPromise(connectionProvider)
		}

		val checkConnection = mockk<CheckIfConnectionIsReadOnly>()
		every { checkConnection.promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
		val filePropertiesContainer = FakeFilePropertiesContainerRepository()
		val revisionProvider = LibraryRevisionProvider(libraryConnectionProvider)
		val filePropertiesProvider =
			FilePropertiesProvider(libraryConnectionProvider, revisionProvider, filePropertiesContainer)

		val filePropertiesPlayStatsUpdater = FilePropertiesPlayStatsUpdater(
			filePropertiesProvider,
			FilePropertyStorage(
				libraryConnectionProvider,
				checkConnection,
				revisionProvider,
				filePropertiesContainer,
				RecordingApplicationMessageBus(),
			)
		)
		Pair(filePropertiesPlayStatsUpdater, filePropertiesProvider)
	}

	private var fileProperties: Map<String, String>? = null
	private val lastPlayed =
		Duration.millis(DateTime.now().plus(Duration.standardDays(10)).millis).standardSeconds

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, filePropertiesProvider) = services
		fileProperties = filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile(serviceFileId))
			.eventually {
				filePropertiesProvider.promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId))
			}
			.toExpiringFuture()
			.get()
	}

	@Test
	fun thenTheLastPlayedIsNotUpdated() {
		assertThat(fileProperties!![KnownFileProperties.LastPlayed]).isEqualTo(lastPlayed.toString())
	}

	@Test
	fun thenTheNumberPlaysIsTheSame() {
		assertThat(fileProperties!![KnownFileProperties.NumberPlays]).isEqualTo("52")
	}
}
