package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnItem.AndItIsSynced

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingTheFiles {
	private val viewModel by lazy {
		val itemProvider = mockk<ProvideItemFiles>().apply {
			every { promiseFiles(LibraryId(516), ItemId(585), FileListParameters.Options.None) } returns listOf(
				ServiceFile(471),
				ServiceFile(469),
				ServiceFile(102),
				ServiceFile(890),
			).toPromise()
		}

		val storedItemAccess = mockk<AccessStoredItems>().apply {
			every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
			every { isItemMarkedForSync(LibraryId(516), Item(585, "king")) } returns true.toPromise()
		}

		FileListViewModel(
            itemProvider,
            storedItemAccess,
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(516), Item(585, "king")).toExpiringFuture().get()
	}

	@Test
	fun thenTheItemIsMarkedForSync() {
		assertThat(viewModel.isSynced.value).isTrue
	}

	@Test
	fun thenTheItemValueIsCorrect() {
		assertThat(viewModel.itemValue.value).isEqualTo("king")
	}

	@Test
	fun `then is loaded is correct`() {
		assertThat(viewModel.isLoading.value).isFalse
	}

	@Test
	fun thenTheLoadedFilesAreCorrect() {
		assertThat(viewModel.files.value)
			.hasSameElementsAs(
				listOf(
					ServiceFile(471),
					ServiceFile(469),
					ServiceFile(102),
					ServiceFile(890),
				)
			)
	}
}
