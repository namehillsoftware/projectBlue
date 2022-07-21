package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.BeforeClass
import org.junit.Test

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
	val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
		every { selectedLibraryId } returns LibraryId(960).toPromise()
	}

	val itemProvider = mockk<ProvideItemFiles>().apply {
		every { promiseFiles(LibraryId(960), ItemId(868), FileListParameters.Options.None) } returns fileList.toPromise()
	}

	val storedItemAccess = mockk<AccessStoredItems>().apply {
		every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
	}

	val controlNowPlaying = mockk<ControlPlaybackService>(relaxUnitFun = true)

	val viewModel = FileListViewModel(
		selectedLibraryIdProvider,
		itemProvider,
		storedItemAccess,
		controlNowPlaying,
	)

	Pair(viewModel, controlNowPlaying)
}

class WhenPlayingTheFiles {

	companion object {
		@BeforeClass
		@JvmStatic
		fun act() {
			val (viewModel, _) = services
			viewModel.loadItem(Item(868, "king")).toExpiringFuture().get()
			viewModel.play()
		}
	}

	@Test
	fun thenPlaybackIsStarted() {
		val (_, playbackControl) = services
		verify(exactly = 1) { playbackControl.startPlaylist(fileList) }
	}
}
