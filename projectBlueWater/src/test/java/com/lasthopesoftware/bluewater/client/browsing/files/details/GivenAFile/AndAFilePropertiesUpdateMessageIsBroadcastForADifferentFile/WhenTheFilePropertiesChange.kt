package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndAFilePropertiesUpdateMessageIsBroadcastForADifferentFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
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
		private const val libraryId = 798
		private const val serviceFileId = "783"
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
							ReadOnlyFileProperty(NormalizedFileProperties.Rating, "815"),
							ReadOnlyFileProperty("little", "more"),
							ReadOnlyFileProperty("evening", "skin"),
							ReadOnlyFileProperty(NormalizedFileProperties.Name, "ahead"),
							ReadOnlyFileProperty(NormalizedFileProperties.Artist, "moon"),
							ReadOnlyFileProperty(NormalizedFileProperties.Album, "number"),
							ReadOnlyFileProperty(NormalizedFileProperties.ImageFile, "battle"),
						)
					) andThen Promise(
						sequenceOf(
							ReadOnlyFileProperty(NormalizedFileProperties.Rating, "7"),
							ReadOnlyFileProperty("bread", "scenery"),
							ReadOnlyFileProperty("rush", "offense"),
							ReadOnlyFileProperty(NormalizedFileProperties.Name, "kiss"),
							ReadOnlyFileProperty(NormalizedFileProperties.Artist, "adoption"),
							ReadOnlyFileProperty(NormalizedFileProperties.Album, "motherly"),
							ReadOnlyFileProperty(NormalizedFileProperties.StackTop, "under"),
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
				},
			)
		)
	}

	@BeforeAll
	fun act() {
		val (messageBus, viewModel) = services

		viewModel.load(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()

		messageBus.sendMessage(FilePropertiesUpdatedMessage(UrlKeyHolder(URL("http://bow"), ServiceFile("937"))))
	}

	@Test
	fun `then the properties are correct`() {
		assertThat(services.second.fileProperties.value.map { Pair(it.property, it.committedValue.value) }).hasSameElementsAs(
			listOf(
				Pair(NormalizedFileProperties.Rating, "815"),
				Pair("little", "more"),
				Pair("evening", "skin"),
				Pair(NormalizedFileProperties.Name, "ahead"),
				Pair(NormalizedFileProperties.Artist, "moon"),
				Pair(NormalizedFileProperties.Album, "number"),
			)
		)
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(services.second.rating.value).isEqualTo(815)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(services.second.artist.value).isEqualTo("moon")
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(services.second.fileName.value).isEqualTo("ahead")
	}

	@Test
	fun `then the album is correct`() {
		assertThat(services.second.album.value).isEqualTo("number")
	}
}
