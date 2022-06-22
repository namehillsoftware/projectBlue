package com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.ProvideNowPlayingFiles
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

private val viewModel by lazy {
	val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
		every { selectedLibraryId } returns LibraryId(163).toPromise()
	}

	val itemProvider = mockk<ProvideItemFiles>().apply {
		every { promiseFiles(LibraryId(163), ItemId(826), FileListParameters.Options.None) } returns listOf(
			ServiceFile(471),
			ServiceFile(469),
			ServiceFile(102),
			ServiceFile(890),
		).toPromise()
	}

	val nowPlayingFileProvider = mockk<ProvideNowPlayingFiles>().apply {
		every { nowPlayingFile } returns ServiceFile(319).toPromise()
	}

	val storedItemAccess = mockk<AccessStoredItems>().apply {
		var isItemMarkedForSync = false
		every { toggleSync(LibraryId(163), ItemId(826), true) } answers {
			isItemMarkedForSync = true
			Unit.toPromise()
		}
		every { isItemMarkedForSync(LibraryId(163), Item(826, "moderate")) } answers { isItemMarkedForSync.toPromise() }
	}

	FileListViewModel(
		mockk(relaxUnitFun = true, relaxed = true),
		selectedLibraryIdProvider,
		itemProvider,
		nowPlayingFileProvider,
		storedItemAccess,
		mockk(),
	)
}

class WhenSyncingTheItem {
	companion object {
		@BeforeClass
		@JvmStatic
		fun act() {
			viewModel.loadItem(Item(826, "moderate")).toExpiringFuture().get()
			viewModel.toggleSync().toExpiringFuture().get()
		}
	}

	@Test
	fun `then item is synced`() {
		assertThat(viewModel.isSynced.value).isTrue
	}
}
