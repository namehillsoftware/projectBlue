package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndThePropertiesAreBeingEdited.AndAPropertyIsModified

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
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

class WhenCancelling {
	companion object {
		private const val libraryId = 918
		private const val serviceFileId = "79"
	}

	private var persistedTrackNumber = ""

	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					sequenceOf(
						FileProperty(KnownFileProperties.Rating, "2"),
						FileProperty("awkward", "prevent"),
						FileProperty("feast", "wind"),
						FileProperty(KnownFileProperties.Name, "please"),
						FileProperty(KnownFileProperties.Artist, "brown"),
						FileProperty(KnownFileProperties.Genre, "subject"),
						FileProperty(KnownFileProperties.Lyrics, "belief"),
						FileProperty(KnownFileProperties.Comment, "pad"),
						FileProperty(KnownFileProperties.Composer, "hotel"),
						FileProperty(KnownFileProperties.Custom, "curl"),
						FileProperty(KnownFileProperties.Publisher, "capital"),
						FileProperty(KnownFileProperties.TotalDiscs, "354"),
						FileProperty(KnownFileProperties.Track, "703"),
						FileProperty(KnownFileProperties.AlbumArtist, "calm"),
						FileProperty(KnownFileProperties.Album, "distant"),
						FileProperty(KnownFileProperties.Date, "1355"),
					)
				)
			},
			mockk {
				every { promiseFileUpdate(LibraryId(libraryId), ServiceFile(serviceFileId), KnownFileProperties.Track, any(), false) } answers {
					persistedTrackNumber = arg(2)
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
			fileProperties.value.first { it.property == KnownFileProperties.Track }.apply {
				updateValue("141")
				cancel()
			}
		}
	}

	@Test
	fun `then the property is not being edited`() {
		assertThat(
			viewModel
				.fileProperties
				.value
				.firstOrNull { it.property == KnownFileProperties.Track }
				?.isEditing
				?.value).isFalse
	}

	@Test
	fun `then the uncommitted property is NOT changed`() {
		assertThat(
			viewModel
				.fileProperties
				.value
				.firstOrNull { it.property == KnownFileProperties.Track }
				?.uncommittedValue
				?.value).isEqualTo("703")
	}

	@Test
	fun `then the committed property is NOT changed`() {
		assertThat(
			viewModel
				.fileProperties
				.value
				.firstOrNull { it.property == KnownFileProperties.Track }
				?.committedValue
				?.value).isEqualTo("703")
	}

	@Test
	fun `then the property change is NOT persisted`() {
		assertThat(persistedTrackNumber).isEmpty()
	}
}
