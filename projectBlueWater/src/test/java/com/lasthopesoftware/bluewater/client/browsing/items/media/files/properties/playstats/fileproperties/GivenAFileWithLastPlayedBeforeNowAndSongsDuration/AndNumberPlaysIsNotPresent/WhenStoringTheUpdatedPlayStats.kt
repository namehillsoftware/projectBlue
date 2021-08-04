package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsNotPresent

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FakeFilePropertiesContainer
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.FilePropertiesStorage
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.SessionFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.library.access.FakeRevisionConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.FakeConnectionResponseTuple
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.BeforeClass
import org.junit.Test

class WhenStoringTheUpdatedPlayStats {

    companion object {
        private var fileProperties: Map<String, String>? = null

		@BeforeClass
		@JvmStatic
        @Throws(InterruptedException::class)
        fun before() {
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
<Field Name="${KnownFileProperties.LAST_PLAYED}">$lastPlayed</Field>
<Field Name="Rating">4</Field>
<Field Name="File Size">2345088</Field>
<Field Name="${KnownFileProperties.DURATION}">$duration</Field>
</Item>
</MPL>
""".toByteArray()
                    )
                },
                "File/GetInfo", "File=23"
            )

            val filePropertiesContainer = FakeFilePropertiesContainer()
            val sessionFilePropertiesProvider =
                SessionFilePropertiesProvider(connectionProvider, filePropertiesContainer)
            val filePropertiesPlayStatsUpdater = FilePropertiesPlayStatsUpdater(
                sessionFilePropertiesProvider,
                FilePropertiesStorage(connectionProvider, filePropertiesContainer)
            )

            val serviceFile = ServiceFile(23)
            fileProperties = filePropertiesPlayStatsUpdater
                .promisePlaystatsUpdate(serviceFile)
                .eventually {
					sessionFilePropertiesProvider.promiseFileProperties(serviceFile)
                }
				.toFuture()
				.get()
        }
    }

	@Test
	fun thenTheLastPlayedIsRecent() {
		assertThat(fileProperties!![KnownFileProperties.LAST_PLAYED]?.toLong()).isCloseTo(Duration.millis(System.currentTimeMillis()).standardSeconds, Offset.offset(10L))
	}

	@Test
	fun thenTheNumberPlaysIsIncremented() {
		assertThat(fileProperties!![KnownFileProperties.NUMBER_PLAYS]).isEqualTo("1")
	}
}
