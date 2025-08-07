package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile.AndAPlaylist

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.NormalizedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.ReadOnlyFileProperty
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
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
		private const val libraryId = 591
		private const val serviceFileId = "338"
		private const val playlistId = "7f50268f-471a-4039-a671-73782e313818"
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
				every { promiseFiles(LibraryId(libraryId), PlaylistId(playlistId)) } returns listOf(
					ServiceFile("830"),
					ServiceFile(serviceFileId),
					ServiceFile("628"),
					ServiceFile("537"),
					ServiceFile("284"),
					ServiceFile("419"),
					ServiceFile("36"),
					ServiceFile("396"),
				).toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		mut.apply {
			load(
				LibraryId(libraryId),
				PlaylistId(playlistId),
				PositionedFile(1, ServiceFile(serviceFileId))
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
			ServiceFile("830"),
			ServiceFile(serviceFileId),
			ServiceFile("628"),
			ServiceFile("537"),
			ServiceFile("284"),
			ServiceFile("419"),
			ServiceFile("36"),
			ServiceFile("396"),
		)
	}

	@Test
	fun `then the playlist is started at the correct position`() {
		assertThat(startedPosition).isEqualTo(1)
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
