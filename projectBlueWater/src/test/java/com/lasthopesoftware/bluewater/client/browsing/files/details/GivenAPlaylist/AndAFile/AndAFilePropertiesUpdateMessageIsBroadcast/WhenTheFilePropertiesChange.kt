package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile.AndAFilePropertiesUpdateMessageIsBroadcast

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FileProperty
import com.lasthopesoftware.bluewater.client.browsing.files.properties.KnownFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertiesUpdatedMessage
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


class WhenTheFilePropertiesChange {
	companion object {
		private const val libraryId = 856
		private const val serviceFileId = "491"
	}

	private val services by lazy {
		val recordingApplicationMessageBus = RecordingApplicationMessageBus()

		Pair(
			recordingApplicationMessageBus,
			FileDetailsViewModel(
				mockk {
					every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
				},
				mockk {
					every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
						sequenceOf(
							FileProperty(KnownFileProperties.Rating, "3"),
							FileProperty("bread", "prevent"),
							FileProperty("silence", "wind"),
							FileProperty(KnownFileProperties.Name, "sorry"),
							FileProperty(KnownFileProperties.Artist, "receive"),
							FileProperty(KnownFileProperties.Album, "part"),
							FileProperty(KnownFileProperties.StackView, "basic"),
						)
					) andThen Promise(
						sequenceOf(
							FileProperty(KnownFileProperties.Rating, "7"),
							FileProperty("bread", "scenery"),
							FileProperty("rush", "offense"),
							FileProperty(KnownFileProperties.Name, "kiss"),
							FileProperty(KnownFileProperties.Artist, "adoption"),
							FileProperty(KnownFileProperties.Album, "motherly"),
							FileProperty(KnownFileProperties.StackTop, "under"),
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
				recordingApplicationMessageBus,
				mockk {
					every {
						promiseUrlKey(
							LibraryId(libraryId),
							ServiceFile(serviceFileId)
						)
					} returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
				}
			)
		)
	}

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = services

		viewModel.loadFromList(
			LibraryId(libraryId),
			listOf(
				ServiceFile("291"),
				ServiceFile("312"),
				ServiceFile("783"),
				ServiceFile("380"),
				ServiceFile(serviceFileId),
				ServiceFile("723"),
				ServiceFile("81"),
				ServiceFile("543"),
			),
			4
		).toExpiringFuture().get()

		messageBus.sendMessage(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId))))
	}

	@Test
	fun `then the properties are correct`() {
		assertThat(services.second.fileProperties.value.map { Pair(it.property, it.committedValue.value) }).hasSameElementsAs(
			listOf(
				Pair(KnownFileProperties.Rating, "7"),
				Pair("bread", "scenery"),
				Pair("rush", "offense"),
				Pair(KnownFileProperties.Name, "kiss"),
				Pair(KnownFileProperties.Artist, "adoption"),
				Pair(KnownFileProperties.Album, "motherly"),
			)
		)
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(services.second.rating.value).isEqualTo(7)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(services.second.artist.value).isEqualTo("adoption")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(services.second.fileName.value).isEqualTo("kiss")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(services.second.album.value).isEqualTo("motherly")
	}
}
