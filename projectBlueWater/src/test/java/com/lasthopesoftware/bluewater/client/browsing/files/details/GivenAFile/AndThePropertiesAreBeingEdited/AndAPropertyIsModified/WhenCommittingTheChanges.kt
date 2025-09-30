package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndThePropertiesAreBeingEdited.AndAPropertyIsModified

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.PassThroughFilePropertiesLookup
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.PassThroughUrlKeyProvider
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

class WhenCommittingTheChanges {
	companion object {
		private const val libraryId = 713
		private const val serviceFileId = "294"
	}

	private var persistedValue = ""

	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					PassThroughFilePropertiesLookup(
						listOf(
							ReadOnlyFileProperty(NormalizedFileProperties.Rating, "2"),
							ReadOnlyFileProperty("awkward", "prevent"),
							ReadOnlyFileProperty("feast", "wind"),
							ReadOnlyFileProperty(NormalizedFileProperties.Name, "please"),
							ReadOnlyFileProperty(NormalizedFileProperties.Artist, "brown"),
							ReadOnlyFileProperty(NormalizedFileProperties.Genre, "subject"),
							ReadOnlyFileProperty(NormalizedFileProperties.Lyrics, "belief"),
							ReadOnlyFileProperty(NormalizedFileProperties.Comment, "pad"),
							ReadOnlyFileProperty(NormalizedFileProperties.Composer, "hotel"),
							ReadOnlyFileProperty(NormalizedFileProperties.Custom, "curl"),
							ReadOnlyFileProperty(NormalizedFileProperties.Publisher, "capital"),
							ReadOnlyFileProperty(NormalizedFileProperties.TotalDiscs, "354"),
							ReadOnlyFileProperty(NormalizedFileProperties.Track, "882"),
							ReadOnlyFileProperty(NormalizedFileProperties.AlbumArtist, "calm"),
							ReadOnlyFileProperty(NormalizedFileProperties.Album, "distant"),
							ReadOnlyFileProperty(NormalizedFileProperties.Date, "1355"),
						)
					)
				)
			},
			mockk {
				every {
					promiseFileUpdate(
						LibraryId(libraryId),
						ServiceFile(serviceFileId),
						NormalizedFileProperties.Track,
						any(),
						true
					)
				} answers {
					persistedValue = arg(3)
					Unit.toPromise()
				}
			},
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
			PassThroughUrlKeyProvider(URL("http://damage")),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.apply {
			load(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
			fileProperties.value.first { it.propertyName == NormalizedFileProperties.Track }
				.apply {
					updateValue("617")
					commitChanges().toExpiringFuture().get()
				}
		}
	}

	@Test
	fun `then the property is not being edited`() {
		assertThat(viewModel.fileProperties.value.firstOrNull { it.propertyName == NormalizedFileProperties.Track }?.isEditing?.value).isFalse
	}

	@Test
	fun `then the committed property is changed`() {
		assertThat(
			viewModel
				.fileProperties
				.value
				.firstOrNull { it.propertyName == NormalizedFileProperties.Track }
				?.committedValue
				?.value).isEqualTo("617")
	}

	@Test
	fun `then the property change is persisted`() {
		assertThat(persistedValue).isEqualTo("617")
	}
}
