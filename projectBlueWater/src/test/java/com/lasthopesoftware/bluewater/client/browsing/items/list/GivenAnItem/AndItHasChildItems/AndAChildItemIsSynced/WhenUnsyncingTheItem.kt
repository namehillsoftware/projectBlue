package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems.AndAChildItemIsSynced

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSyncingTheItem {

	private val viewModel by lazy {
		val selectedLibraryIdProvider = mockk<ProvideSelectedLibraryId>().apply {
			every { selectedLibraryId } returns LibraryId(707).toPromise()
		}

		val itemProvider = mockk<ProvideItemFiles>().apply {
			every { promiseFiles(LibraryId(707), ItemId(501), FileListParameters.Options.None) } returns listOf(
				ServiceFile(471),
				ServiceFile(469),
				ServiceFile(102),
				ServiceFile(890),
			).toPromise()
		}

		val storedItemAccess = mockk<AccessStoredItems>().apply {
			var isItemMarkedForSync = true
			every { toggleSync(LibraryId(707), ItemId(501), false) } answers {
				isItemMarkedForSync = false
				Unit.toPromise()
			}
			every { isItemMarkedForSync(LibraryId(707), Item(501, "observe")) } answers { isItemMarkedForSync.toPromise() }
		}

		FileListViewModel(
			selectedLibraryIdProvider,
			itemProvider,
			storedItemAccess,
			mockk(),
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(Item(501, "observe")).toExpiringFuture().get()
		viewModel.toggleSync().toExpiringFuture().get()
	}

	@Test
	fun `then item is not synced`() {
		assertThat(viewModel.isSynced.value).isFalse
	}
}
