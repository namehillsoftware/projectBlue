package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndLowerCaseFileProperties

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

class WhenLoading {

	companion object {
		private const val libraryId = 369
		private const val serviceFileId = "8FBRCxlaU4"
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
                        ReadOnlyFileProperty(NormalizedFileProperties.Rating.lowercase(), "218"),
                        ReadOnlyFileProperty("Natoquefacilisis", "Semperlobortis"),
                        ReadOnlyFileProperty("Vivamusdapibus", "Placeratinceptos"),
                        ReadOnlyFileProperty(NormalizedFileProperties.Name.lowercase(), "Vulputateornare"),
                        ReadOnlyFileProperty(NormalizedFileProperties.Artist.lowercase(), "Consequattempor"),
                        ReadOnlyFileProperty(NormalizedFileProperties.Album.lowercase(), "Dignissimorci"),
                        ReadOnlyFileProperty(NormalizedFileProperties.Composer.lowercase(), "Facilisimalesuada"),
                        ReadOnlyFileProperty(NormalizedFileProperties.AlbumArtist.lowercase(), "Condimentummaecenas"),
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
                } returns byteArrayOf(553.toByte(), 237.toByte()).toPromise()
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
		assertThat(viewModel.fileProperties.value.map {
            Pair(
                it.property,
                it.committedValue.value
            )
        }).hasSameElementsAs(
			listOf(
				Pair(NormalizedFileProperties.Rating, "218"),
				Pair("Natoquefacilisis", "Semperlobortis"),
				Pair("Vivamusdapibus", "Placeratinceptos"),
				Pair(NormalizedFileProperties.Name, "Vulputateornare"),
				Pair(NormalizedFileProperties.Artist, "Consequattempor"),
				Pair(NormalizedFileProperties.Album, "Dignissimorci"),
				Pair(NormalizedFileProperties.Composer, "Facilisimalesuada"),
				Pair(NormalizedFileProperties.AlbumArtist, "Condimentummaecenas"),
			)
		)
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(viewModel.rating.value).isEqualTo(218)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(viewModel.artist.value).isEqualTo("Consequattempor")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(viewModel.fileName.value).isEqualTo("Vulputateornare")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(viewModel.album.value).isEqualTo("Dignissimorci")
	}
}
