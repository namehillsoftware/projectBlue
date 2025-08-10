package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndAPropertyIsHighlighted

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
		private const val libraryId = 72
		private const val serviceFileId = "300"
	}

	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					sequenceOf(
						ReadOnlyFileProperty(NormalizedFileProperties.Rating, "412"),
						ReadOnlyFileProperty("simple", "middle"),
						ReadOnlyFileProperty("aside", "vessel"),
						ReadOnlyFileProperty(NormalizedFileProperties.Name, "skin"),
						ReadOnlyFileProperty(NormalizedFileProperties.Artist, "afford"),
						ReadOnlyFileProperty(NormalizedFileProperties.Genre, "avenue"),
						ReadOnlyFileProperty(NormalizedFileProperties.Lyrics, "regret"),
						ReadOnlyFileProperty(NormalizedFileProperties.Comment, "dream"),
						ReadOnlyFileProperty(NormalizedFileProperties.Composer, "risk"),
						ReadOnlyFileProperty(NormalizedFileProperties.Custom, "fate"),
						ReadOnlyFileProperty(NormalizedFileProperties.Publisher, "crash"),
						ReadOnlyFileProperty(NormalizedFileProperties.TotalDiscs, "bone"),
						ReadOnlyFileProperty(NormalizedFileProperties.Track, "passage"),
						ReadOnlyFileProperty(NormalizedFileProperties.AlbumArtist, "enclose"),
						ReadOnlyFileProperty(NormalizedFileProperties.Album, "amuse"),
						ReadOnlyFileProperty(NormalizedFileProperties.Date, "9357"),
					)
				)
			},
			mockk(),
			mockk {
				every { promiseImageBytes() } returns byteArrayOf(3, 4).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), any<ServiceFile>()) } returns byteArrayOf(
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
		viewModel.apply {
			load(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
			fileProperties.value.first { it.property == NormalizedFileProperties.Date }.apply {
				highlight()
				cancel()
			}
		}
	}

	@Test
	fun `then there is no highlighted property`() {
		assertThat(viewModel.highlightedProperty.value).isNull()
	}
}
