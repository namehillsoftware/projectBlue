package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndAQuery

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
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

// Needed for image bytes
class WhenPlayingFromFileDetails {

	companion object {
		private const val libraryId = 299
		private const val serviceFileId = "cgxDh6Qn"
		private const val searchQuery = "O5poI8WP2q"
	}

	private lateinit var startedLibraryId: LibraryId
	private lateinit var startedList: List<ServiceFile>
	private var startedPosition = -1

	private val mut by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					sequenceOf(
						ReadOnlyFileProperty(NormalizedFileProperties.Name, "toward"),
						ReadOnlyFileProperty(NormalizedFileProperties.Artist, "load"),
						ReadOnlyFileProperty(NormalizedFileProperties.Album, "square"),
						ReadOnlyFileProperty(NormalizedFileProperties.Rating, "4"),
						ReadOnlyFileProperty("razor", "through"),
						ReadOnlyFileProperty("smile", "since"),
						ReadOnlyFileProperty("harvest", "old"),
					)
				)
			},
			mockk(),
			mockk {
				every { promiseImageBytes() } returns byteArrayOf(111, 112).toPromise()
			},
			mockk {
				every { promiseImageBytes(LibraryId(libraryId), any<ServiceFile>()) } returns byteArrayOf(322.toByte(), 480.toByte()).toPromise()
			},
			mockk {
				every { startPlaylist(LibraryId(libraryId), any<List<ServiceFile>>(), any()) } answers {
					startedLibraryId = firstArg()
					startedList = secondArg()
					startedPosition = lastArg()
				}
			},
			RecordingApplicationMessageBus(),
			mockk {
				every { promiseUrlKey(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
			},
			mockk {
				every { promiseAudioFiles(LibraryId(libraryId), searchQuery) } returns listOf(
					ServiceFile("cd8a407d-7afb-4e1a-9f89-8b81f902375b"),
					ServiceFile("2eb367f1-8713-449e-9bec-0e326169d778"),
					ServiceFile(serviceFileId),
					ServiceFile("17f00a96-00c2-4d06-aef9-3f7cacc9812c"),
					ServiceFile("606ce37e-006a-47e7-928c-71b65ffc587f"),
					ServiceFile("82b138b5-7be1-4352-b825-48087ca8e1aa"),
					ServiceFile("f76c2f3c-339b-42df-93e4-7e4e980de65c"),
					ServiceFile("6eb75879-ab8e-4707-bc7e-7dca8d0ed843"),
				).toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		mut.apply {
			load(
				LibraryId(libraryId),
				searchQuery,
				PositionedFile(2, ServiceFile(serviceFileId))
			).toExpiringFuture().get()

			play()
		}
	}

	@Test
	fun `then the started library id is correct`() {
		assertThat(startedLibraryId).isEqualTo(LibraryId(libraryId))
	}

	@Test
	fun `then the correct playlist is started`() {
		assertThat(startedList).containsExactlyInAnyOrder(
			ServiceFile("cd8a407d-7afb-4e1a-9f89-8b81f902375b"),
			ServiceFile("2eb367f1-8713-449e-9bec-0e326169d778"),
			ServiceFile(serviceFileId),
			ServiceFile("17f00a96-00c2-4d06-aef9-3f7cacc9812c"),
			ServiceFile("606ce37e-006a-47e7-928c-71b65ffc587f"),
			ServiceFile("82b138b5-7be1-4352-b825-48087ca8e1aa"),
			ServiceFile("f76c2f3c-339b-42df-93e4-7e4e980de65c"),
			ServiceFile("6eb75879-ab8e-4707-bc7e-7dca8d0ed843"),
		)
	}

	@Test
	fun `then the playlist is started at the correct position`() {
		assertThat(startedPosition).isEqualTo(2)
	}

	@Test
	fun `then the artist is correct`() {
		assertThat(mut.artist.value).isEqualTo("load")
	}

	@Test
	fun `then the rating is correct`() {
		assertThat(mut.rating.value).isEqualTo(4)
	}

	@Test
	fun `then the file name is correct`() {
		assertThat(mut.fileName.value).isEqualTo("toward")
	}

	@Test
	fun `then the file properties are correct`() {
		assertThat(mut.fileProperties.value.map { Pair(it.property, it.committedValue.value) }).containsExactlyInAnyOrder(
			Pair(NormalizedFileProperties.Name, "toward"),
			Pair(NormalizedFileProperties.Artist, "load"),
			Pair(NormalizedFileProperties.Album, "square"),
			Pair(NormalizedFileProperties.Rating, "4"),
			Pair("razor", "through"),
			Pair("smile", "since"),
			Pair("harvest", "old"),
		)
	}
}
