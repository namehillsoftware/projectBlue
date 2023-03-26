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
import io.mockk.verify
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenPlayingTheFiles {

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

		val controlNowPlaying = mockk<ControlPlaybackService>(relaxUnitFun = true)

		val viewModel = FileListViewModel(
            itemProvider,
            storedItemAccess,
            controlNowPlaying,
		)

		Pair(viewModel, controlNowPlaying)
	}

	@BeforeAll
	fun act() {
		val (viewModel, _) = services
		viewModel.loadItem(LibraryId(960), Item(868, "king")).toExpiringFuture().get()
		viewModel.play()
	}

	@Test
	fun thenPlaybackIsStarted() {
		val (_, playbackControl) = services
		verify(exactly = 1) { playbackControl.startPlaylist(fileList) }
	}
}
