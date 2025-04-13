package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndThePropertiesAreBeingEdited.AndAPropertyIsModified

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
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
					sequenceOf(
						FileProperty(NormalizedFileProperties.Rating, "2"),
						FileProperty("awkward", "prevent"),
						FileProperty("feast", "wind"),
						FileProperty(NormalizedFileProperties.Name, "please"),
						FileProperty(NormalizedFileProperties.Artist, "brown"),
						FileProperty(NormalizedFileProperties.Genre, "subject"),
						FileProperty(NormalizedFileProperties.Lyrics, "belief"),
						FileProperty(NormalizedFileProperties.Comment, "pad"),
						FileProperty(NormalizedFileProperties.Composer, "hotel"),
						FileProperty(NormalizedFileProperties.Custom, "curl"),
						FileProperty(NormalizedFileProperties.Publisher, "capital"),
						FileProperty(NormalizedFileProperties.TotalDiscs, "354"),
						FileProperty(NormalizedFileProperties.Track, "882"),
						FileProperty(NormalizedFileProperties.AlbumArtist, "calm"),
						FileProperty(NormalizedFileProperties.Album, "distant"),
						FileProperty(NormalizedFileProperties.Date, "1355"),
					)
				)
			},
			mockk {
				every { promiseFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId), NormalizedFileProperties.Track, any(), true) } answers {
					persistedValue = arg(3)
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

	@BeforeAll
	fun act() {
		viewModel.apply {
			loadFromList(LibraryId(libraryId), listOf(ServiceFile(serviceFileId)), 0).toExpiringFuture().get()
			fileProperties.value.first { it.property == NormalizedFileProperties.Track }
				.apply {
					updateValue("617")
					commitChanges().toExpiringFuture().get()
				}
		}
	}

	@Test
	fun `then the property is not being edited`() {
		assertThat(viewModel.fileProperties.value.firstOrNull { it.property == NormalizedFileProperties.Track }?.isEditing?.value).isFalse
	}

	@Test
	fun `then the committed property is changed`() {
		assertThat(
			viewModel
				.fileProperties
				.value
				.firstOrNull { it.property == NormalizedFileProperties.Track }
				?.committedValue
				?.value).isEqualTo("617")
	}

	@Test
	fun `then the property change is persisted`() {
		assertThat(persistedValue).isEqualTo("617")
	}
}
