package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenLoadingTheFiles {

	private val viewModel by lazy {
		val itemProvider = mockk<ProvideItemFiles>().apply {
			every { promiseFiles(LibraryId(516), ItemId("585")) } returns listOf(
				ServiceFile("471"),
				ServiceFile("469"),
				ServiceFile("102"),
				ServiceFile("890"),
			).toPromise()
		}

		val storedItemAccess = mockk<AccessStoredItems>().apply {
			every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
		}

		FileListViewModel(
            itemProvider,
            storedItemAccess,
		)
	}

	@BeforeAll
	fun act() {
		viewModel.loadItem(LibraryId(516), Item("585", "king")).toExpiringFuture().get()
	}

	@Test
	fun thenTheItemIsNotMarkedForSync() {
		assertThat(viewModel.isSynced.value).isFalse
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
					ServiceFile("471"),
					ServiceFile("469"),
					ServiceFile("102"),
					ServiceFile("890"),
				)
			)
	}
}
