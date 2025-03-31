package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.GivenAFileWithLastPlayedInTheFuture

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
		private const val serviceFileId = "894"
	}

	private val services by lazy {
		val fileProperties = mutableMapOf(
			Pair(KnownFileProperties.LastPlayed, lastPlayed.toString()),
			Pair(KnownFileProperties.NumberPlays, 52.toString()),
		)

		val filePropertiesPlayStatsUpdater = FilePropertiesPlayStatsUpdater(
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns fileProperties.toPromise()
			},
			mockk {
				every { promiseFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId), any(), any(), false) } answers {
					fileProperties[thirdArg()] = arg(4)
					Unit.toPromise()
				}
			}
		)
		Pair(filePropertiesPlayStatsUpdater, fileProperties)
	}

	private var fileProperties: Map<String, String>? = null
	private val lastPlayed =
		Duration.millis(DateTime.now().plus(Duration.standardDays(10)).millis).standardSeconds

	@BeforeAll
	fun before() {
		val (filePropertiesPlayStatsUpdater, fileProperties) = services
		this.fileProperties = fileProperties
		filePropertiesPlayStatsUpdater
			.promisePlaystatsUpdate(LibraryId(libraryId), ServiceFile(serviceFileId))
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
