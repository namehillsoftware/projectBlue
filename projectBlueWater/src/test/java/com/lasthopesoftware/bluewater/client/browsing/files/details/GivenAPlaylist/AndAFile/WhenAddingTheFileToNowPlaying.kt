package com.lasthopesoftware.bluewater.client.browsing.files.details.GivenAPlaylist.AndAFile

import android.graphics.BitmapFactory
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Test
import java.net.URL

private const val libraryId = 275
private const val serviceFileId = 860

class WhenAddingTheFileToNowPlaying : AndroidContext() {
	companion object {

		private var addedLibraryId: LibraryId? = null
		private var addedServiceFile: ServiceFile? = null

		private var viewModel: Lazy<FileDetailsViewModel>? = lazy {
			FileDetailsViewModel(
				mockk {
					every { promiseIsReadOnly(LibraryId(libraryId)) } returns false.toPromise()
				},
				mockk {
					every { promiseFileProperties(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns Promise(emptySequence())
				},
				mockk(),
				mockk {
					every { promiseFileBitmap() } returns BitmapFactory
						.decodeByteArray(byteArrayOf(3, 4), 0, 2)
						.toPromise()
				},
				mockk {
					every { promiseFileBitmap(LibraryId(libraryId), any()) } returns BitmapFactory
						.decodeByteArray(byteArrayOf(61, 127), 0, 2)
						.toPromise()
				},
				mockk {
					every { addToPlaylist(any(), any()) } answers {
						addedLibraryId = firstArg()
						addedServiceFile = lastArg()
					}
				},
				RecordingApplicationMessageBus(),
				mockk {
					every { promiseUrlKey(LibraryId(libraryId), ServiceFile(serviceFileId)) } returns UrlKeyHolder(URL("http://bow"), ServiceFile(serviceFileId)).toPromise()
				},
			)
		}

		@JvmStatic
		@AfterClass
		fun cleanup() {
			viewModel = null
			addedServiceFile = null
		}
	}

	override fun before() {
		viewModel?.value?.loadFromList(
			LibraryId(libraryId),
			listOf(
				ServiceFile(291),
				ServiceFile(312),
				ServiceFile(783),
				ServiceFile(380),
				ServiceFile(serviceFileId),
				ServiceFile(723),
				ServiceFile(81),
				ServiceFile(543),
			),
			4
		)?.toExpiringFuture()?.get()
		viewModel?.value?.addToNowPlaying()
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
