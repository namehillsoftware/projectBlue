package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndTheConnectionIsReadOnly

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinition
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertyType
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
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

class WhenAnotherPropertyIsEdited {
	companion object {
		private const val libraryId = 792
		private const val serviceFileId = "220"
	}

	private var persistedValue = ""

	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns true.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					sequenceOf(
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
						ReadOnlyFileProperty(NormalizedFileProperties.Band, "stair"),
					)
				)
			},
			mockk {
				every { promiseFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId), NormalizedFileProperties.Custom, any(), false) } answers {
					persistedValue = arg(2)
					Unit.toPromise()
				}
			},
			mockk {
				every { promiseImageBytes() } returns byteArrayOf(3, 4).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), any<ServiceFile>()) } returns byteArrayOf(61, 127).toPromise()
			},
			mockk(),
			RecordingApplicationMessageBus(),
			PassThroughUrlKeyProvider(URL("http://damage")),
		)
	}

	private val FileDetailsViewModel.propertyToEdit
		get() = fileProperties.value.first { it.property == EditableFilePropertyDefinition.Band.propertyName }

	@BeforeAll
	fun act() {
		viewModel.apply {
			loadFromList(LibraryId(libraryId), listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
			propertyToEdit.apply {
				highlight()
				edit()
			}
		}
	}

	@Test
	fun `then the property has the correct editable type`() {
		assertThat(viewModel.propertyToEdit.editableType).isEqualTo(FilePropertyType.ShortFormText)
	}

	@Test
	fun `then the property is NOT being edited`() {
		assertThat(viewModel.propertyToEdit.isEditing.value).isFalse
	}

	@Test
	fun `then the new property is highlighted`() {
		assertThat(viewModel.highlightedProperty.value).isEqualTo(viewModel.propertyToEdit)
	}
}
