package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedInTheFuture

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ScopedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionConnectionProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.ScopedRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.client.connection.authentication.CheckIfScopedConnectionIsReadOnly
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenStoringTheUpdatedPlayStats {

    companion object {
        private var fileProperties: Map<String, String>? = null
        private val lastPlayed =
            Duration.millis(DateTime.now().plus(Duration.standardDays(10)).millis).standardSeconds

        @BeforeClass
        @JvmStatic
        fun before() {
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
<Field Name="${KnownFileProperties.LAST_PLAYED}">$lastPlayed</Field>
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
            val filePropertiesContainer = FakeFilePropertiesContainer()
			val checkScopedRevisions = ScopedRevisionProvider(connectionProvider)
			val scopedFilePropertiesProvider = ScopedFilePropertiesProvider(
                connectionProvider,
				checkScopedRevisions,
                filePropertiesContainer
            )
			val checkConnection = mockk<CheckIfScopedConnectionIsReadOnly>()
			every { checkConnection.promiseIsReadOnly() } returns false.toPromise()
            val filePropertiesPlayStatsUpdater = FilePropertiesPlayStatsUpdater(
                scopedFilePropertiesProvider,
                ScopedFilePropertiesStorage(connectionProvider, checkConnection, checkScopedRevisions, filePropertiesContainer)
            )
			fileProperties = filePropertiesPlayStatsUpdater
                .promisePlaystatsUpdate(ServiceFile(23))
                .eventually {
					scopedFilePropertiesProvider.promiseFileProperties(ServiceFile(23))
                }
				.toFuture()
				.get()
        }
    }

	@Test
	fun thenTheLastPlayedIsNotUpdated() {
		assertThat(fileProperties!![KnownFileProperties.LAST_PLAYED]).isEqualTo(lastPlayed.toString())
	}

	@Test
	fun thenTheNumberPlaysIsTheSame() {
		assertThat(fileProperties!![KnownFileProperties.NUMBER_PLAYS]).isEqualTo("52")
	}
}
