package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatterBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

private const val libraryId = 944
private const val serviceFileId = 161

class WhenLoading {
	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					sequenceOf(
						FileProperty(KnownFileProperties.Rating, "3"),
						FileProperty("too", "prevent"),
						FileProperty("shirt", "wind"),
						FileProperty(KnownFileProperties.Name, "holiday"),
						FileProperty(KnownFileProperties.Artist, "board"),
						FileProperty(KnownFileProperties.Album, "virtue"),
						FileProperty(KnownFileProperties.DateCreated, "1592510356")
					)
				)
			},
			mockk(),
			mockk {
				every { promiseImageBytes() } returns byteArrayOf(3, 4).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), any<ServiceFile>()) } returns byteArrayOf(61, 127).toPromise()
			},
			mockk(),
			RecordingApplicationMessageBus(),
			mockk {
				every { promiseUrlKey(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadFromList(LibraryId(libraryId), listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
	}

	@Test
	fun `then the properties are correct`() {
		assertThat(viewModel.fileProperties?.value?.map { Pair(it.property, it.committedValue.value) }).hasSameElementsAs(
			listOf(
				Pair(KnownFileProperties.Rating, "3"),
				Pair("too", "prevent"),
				Pair("shirt", "wind"),
				Pair(KnownFileProperties.Name, "holiday"),
				Pair(KnownFileProperties.Artist, "board"),
				Pair(KnownFileProperties.Album, "virtue"),
				Pair(KnownFileProperties.DateCreated, DateTime(1592510356L * 1000).toString(DateTimeFormatterBuilder()
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
		assertThat(viewModel.rating?.value).isEqualTo(3)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist?.value).isEqualTo("board")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(viewModel.fileName?.value).isEqualTo("holiday")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(viewModel.album?.value).isEqualTo("virtue")
	}
}
