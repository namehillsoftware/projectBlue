package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlayingTheFilesShuffled {

	private var playedFileList: List<ServiceFile> = emptyList()

	private val fileList by lazy {
		listOf(
			ServiceFile(82),
			ServiceFile(811),
			ServiceFile(370),
			ServiceFile(337),
			ServiceFile(896),
		)
	}

	private val services by lazy {
		val itemProvider = mockk<ProvideItemFiles>().apply {
			every { promiseFiles(LibraryId(960), ItemId(868), FileListParameters.Options.None) } returns fileList.toPromise()
		}

		val storedItemAccess = mockk<AccessStoredItems>().apply {
			every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
		}

		val controlNowPlaying = mockk<ControlPlaybackService>().apply {
			every { startPlaylist(any<List<ServiceFile>>(), any()) } answers {
				playedFileList = firstArg()
			}
		}

		val viewModel = FileListViewModel(
            itemProvider,
            storedItemAccess,
            controlNowPlaying,
		)

		viewModel
	}

	@BeforeAll
	fun act() {
		val viewModel = services
		viewModel.loadItem(LibraryId(960), Item(868, "king")).toExpiringFuture().get()
		viewModel.playShuffled().toExpiringFuture().get()
	}

	@Test
	fun thenPlaybackIsStarted() {
		assertThat(playedFileList).hasSameElementsAs(fileList)
	}
}
