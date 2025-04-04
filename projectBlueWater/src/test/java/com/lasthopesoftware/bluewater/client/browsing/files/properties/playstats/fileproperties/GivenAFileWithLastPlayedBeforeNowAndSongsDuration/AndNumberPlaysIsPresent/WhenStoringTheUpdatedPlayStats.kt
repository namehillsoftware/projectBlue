package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedBeforeNowAndSongsDuration.AndNumberPlaysIsPresent

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenStoringTheUpdatedPlayStats {

	companion object {
		private const val libraryId = 548
		private const val serviceFileId = "23"
	}

	private val services by lazy {
		val duration = Duration.standardMinutes(5).millis
		originalLastPlayed = Duration.millis(DateTime.now().minus(Duration.standardDays(10)).millis).standardSeconds
		val fileProperties = mutableMapOf(
			Pair(KnownFileProperties.LastPlayed, originalLastPlayed.toString()),
			Pair(KnownFileProperties.NumberPlays, 52.toString()),
			Pair(KnownFileProperties.Duration, duration.toString()),
		)

		Pair(
			FilePropertiesPlayStatsUpdater(
				mockk {
					every { promiseFileProperties(LibraryId(libraryId), ServiceFile(
						serviceFileId
					)) } returns fileProperties.toPromise()
				},
				mockk {
					every { promiseFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId), any(), any(), false) } answers {
						fileProperties[thirdArg()] = arg(3)
						Unit.toPromise()
					}
				}
			),
			fileProperties,
		)
	}

	private var fileProperties: Map<String, String>? = null
	private var originalLastPlayed: Long = 0

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, sessionFileProperties) = services
		fileProperties = sessionFileProperties
		filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile("23"))
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
