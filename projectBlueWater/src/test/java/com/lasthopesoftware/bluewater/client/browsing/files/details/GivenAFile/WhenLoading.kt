package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenLoading {

	companion object {
		private const val libraryId = 944
		private const val serviceFileId = "161"
	}

	private val viewModel by lazy {
        FileDetailsViewModel(
            mockk {
                every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
            },
            mockk {
                every {
                    promiseFileProperties(
                        LibraryId(libraryId),
                        ServiceFile(serviceFileId)
                    )
                } returns Promise(
                    sequenceOf(
                        ReadOnlyFileProperty(NormalizedFileProperties.Rating, "3"),
                        ReadOnlyFileProperty("too", "prevent"),
                        ReadOnlyFileProperty("shirt", "wind"),
                        ReadOnlyFileProperty(NormalizedFileProperties.Name, "holiday"),
                        ReadOnlyFileProperty(NormalizedFileProperties.Artist, "board"),
                        ReadOnlyFileProperty(NormalizedFileProperties.Album, "virtue"),
                        ReadOnlyFileProperty(NormalizedFileProperties.DateCreated, "1592510356")
                    )
                )
            },
            mockk(),
            mockk {
                every { promiseImageBytes() } returns byteArrayOf(3, 4).toPromise()
            },
            mockk {
                every {
                    promiseImageBytes(
                        LibraryId(libraryId),
                        any<ServiceFile>()
                    )
                } returns byteArrayOf(
                    61,
                    127
                ).toPromise()
            },
            mockk(),
            RecordingApplicationMessageBus(),
            mockk {
                every {
                    promiseUrlKey(
                        LibraryId(libraryId),
                        ServiceFile(serviceFileId)
                    )
                } returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
            },
        )
	}

	@BeforeAll
	fun act() {
		viewModel
			.load(LibraryId(libraryId), ServiceFile(serviceFileId))
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the properties are correct`() {
		Assertions.assertThat(viewModel.fileProperties.value.map {
            Pair(
                it.property,
                it.committedValue.value
            )
        }).hasSameElementsAs(
			listOf(
				Pair(NormalizedFileProperties.Rating, "3"),
				Pair("too", "prevent"),
				Pair("shirt", "wind"),
				Pair(NormalizedFileProperties.Name, "holiday"),
				Pair(NormalizedFileProperties.Artist, "board"),
				Pair(NormalizedFileProperties.Album, "virtue"),
				Pair(
                    NormalizedFileProperties.DateCreated, DateTime(1592510356L * 1000).toString(
                        DateTimeFormatterBuilder()
					.appendMonthOfYear(1)
					.appendLiteral('/')
					.appendDayOfMonth(1)
					.appendLiteral('/')
					.appendYear(4, 4)
					.appendLiteral(" at ")
					.appendClockhourOfHalfday(1)
					.appendLiteral(':')
					.appendMinuteOfHour(2)
					.appendLiteral(' ')
					.appendHalfdayOfDayText()
					.toFormatter()))
			)
		)
	}

	@Test
	fun `then the rating is correct`() {
		Assertions.assertThat(viewModel.rating.value).isEqualTo(3)
	}

	@Test
	fun `then the artist is correct`() {
		Assertions.assertThat(viewModel.artist.value).isEqualTo("board")
	}

	@Test
	fun `then the file name is correct`() {
		Assertions.assertThat(viewModel.fileName.value).isEqualTo("holiday")
	}

	@Test
	fun `then the album is correct`() {
		Assertions.assertThat(viewModel.album.value).isEqualTo("virtue")
	}
}
