package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndPropertiesAreLoaded

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.PassThroughFilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.observables.mapNotNull
import com.lasthopesoftware.observables.toCloseable
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

class `When Reloading` {

	companion object {
		private const val libraryId = 446
		private const val serviceFileId = "15a5341f83924404b9ec04573d1e1862"
	}

	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					PassThroughFilePropertiesLookup(
						listOf(
							ReadOnlyFileProperty(NormalizedFileProperties.Rating, "3"),
							ReadOnlyFileProperty("too", "prevent"),
							ReadOnlyFileProperty("shirt", "wind"),
							ReadOnlyFileProperty(NormalizedFileProperties.Name, "holiday"),
							ReadOnlyFileProperty(NormalizedFileProperties.Artist, "board"),
							ReadOnlyFileProperty(NormalizedFileProperties.Album, "virtue"),
							ReadOnlyFileProperty(NormalizedFileProperties.DateCreated, "1592510356")
						)
					)
				) andThen Promise(
					PassThroughFilePropertiesLookup(
						listOf(
							ReadOnlyFileProperty(NormalizedFileProperties.Rating, "729"),
							ReadOnlyFileProperty("gCaqE9Z", "I03s2HREwCY"),
							ReadOnlyFileProperty("n2naBbP", "1TcgZ1nsRN"),
							ReadOnlyFileProperty(NormalizedFileProperties.Name, "Namfusce"),
							ReadOnlyFileProperty(NormalizedFileProperties.Artist, "EkaterinaYu"),
							ReadOnlyFileProperty(NormalizedFileProperties.Album, "Quisquecras"),
							ReadOnlyFileProperty(NormalizedFileProperties.DateCreated, "2272510356")
						)
					)
				)
			},
			mockk(),
			mockk {
				every { promiseImageBytes() } returns byteArrayOf(7, 7).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), any<ServiceFile>()) } returns byteArrayOf(
					90,
					127,
					89
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

	private val loadingStates = mutableListOf<Boolean>()

	@BeforeAll
	fun act() {
		viewModel.isLoading.mapNotNull().subscribe(loadingStates::add).toCloseable().use {
			viewModel.load(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
			viewModel.promiseLoadedActiveFile().toExpiringFuture().get()
		}
	}

	@Test
	fun `then the loading states are correct`() {
		assertThat(loadingStates).containsExactly(false, true, false, true, false)
	}

	@Test
	fun `then the properties are correct`() {
		assertThat(viewModel.fileProperties.value.map { Pair(it.propertyName, it.committedValue.value) }).hasSameElementsAs(
			listOf(
				Pair(NormalizedFileProperties.Rating, "729"),
				Pair("gCaqE9Z", "I03s2HREwCY"),
				Pair("n2naBbP", "1TcgZ1nsRN"),
				Pair(NormalizedFileProperties.Name, "Namfusce"),
				Pair(NormalizedFileProperties.Artist, "EkaterinaYu"),
				Pair(NormalizedFileProperties.Album, "Quisquecras"),
				Pair(NormalizedFileProperties.DateCreated, DateTime(2272510356L * 1000).toString(DateTimeFormatterBuilder()
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
		assertThat(viewModel.rating.value).isEqualTo(729)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("EkaterinaYu")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(viewModel.fileName.value).isEqualTo("Namfusce")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(viewModel.album.value).isEqualTo("Quisquecras")
	}
}
