package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.URL

class WhenHighlightingTheProperty {
	companion object {
		private const val libraryId = 443
		private const val serviceFileId = "485"

		private val viewModel by lazy {
			FileDetailsViewModel(
				mockk {
					every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
				},
				mockk {
					every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
						sequenceOf(
							ReadOnlyFileProperty(NormalizedFileProperties.Rating, "947"),
							ReadOnlyFileProperty("sound", "wave"),
							ReadOnlyFileProperty("creature", "concern"),
							ReadOnlyFileProperty(NormalizedFileProperties.Name, "mat"),
							ReadOnlyFileProperty(NormalizedFileProperties.Artist, "send"),
							ReadOnlyFileProperty(NormalizedFileProperties.Genre, "rush"),
							ReadOnlyFileProperty(NormalizedFileProperties.Lyrics, "reach"),
							ReadOnlyFileProperty(NormalizedFileProperties.Comment, "police"),
							ReadOnlyFileProperty(NormalizedFileProperties.Composer, "present"),
							ReadOnlyFileProperty(NormalizedFileProperties.Custom, "steel"),
							ReadOnlyFileProperty(NormalizedFileProperties.Publisher, "lipstick"),
							ReadOnlyFileProperty(NormalizedFileProperties.TotalDiscs, "small"),
							ReadOnlyFileProperty(NormalizedFileProperties.Track, "anxious"),
							ReadOnlyFileProperty(NormalizedFileProperties.AlbumArtist, "date"),
							ReadOnlyFileProperty(NormalizedFileProperties.Album, "ever"),
							ReadOnlyFileProperty(NormalizedFileProperties.Date, "9"),
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
				mockk(),
			)
		}
	}

	@BeforeAll
	fun act() {
		viewModel.apply {
			loadFromList(LibraryId(libraryId), listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
			fileProperties.value.first { it.property == NormalizedFileProperties.Publisher }.highlight()
		}
	}

	@Test
	fun `then the highlighted property is correct`() {
		assertThat(viewModel.highlightedProperty.value?.property).isEqualTo(NormalizedFileProperties.Publisher)
	}

	@Test
	fun `then the highlighted property value is correct`() {
		assertThat(viewModel.highlightedProperty.value?.committedValue?.value).isEqualTo("lipstick")
	}
}
