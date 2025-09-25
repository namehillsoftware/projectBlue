package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAFile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
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

class `When Adding The File As Next To Play` {

	companion object {
		private const val libraryId = 250
		private const val serviceFileId = "LoYEFU8fW"
	}

	private var addedLibraryId: LibraryId? = null
	private var addedServiceFile: ServiceFile? = null

	private val viewModel by lazy {
		FileDetailsViewModel(
			mockk {
				every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
			},
			mockk {
				every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(
					emptySequence()
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
			mockk {
				every { addAfterNowPlayingFile(any(), any()) } answers {
					addedLibraryId = firstArg()
					addedServiceFile = lastArg()
				}
			},
			RecordingApplicationMessageBus(),
			mockk {
				every {
					promiseUrlKey(
						LibraryId(libraryId),
						ServiceFile(serviceFileId)
					)
				} returns UrlKeyHolder(URL("http://Sollicitudinac"), ServiceFile(serviceFileId)).toPromise()
			},
		)
	}

	@BeforeAll
	fun act() {
		viewModel.load(LibraryId(libraryId), ServiceFile(serviceFileId)).toExpiringFuture().get()
		viewModel.playNext()
	}

	@Test
	fun `then the file is added with the correct library id`() {
		assertThat(addedLibraryId).isEqualTo(LibraryId(libraryId))
	}

	@Test
	fun `then the file is added to now playing`() {
		assertThat(addedServiceFile).isEqualTo(ServiceFile(serviceFileId))
	}
}
