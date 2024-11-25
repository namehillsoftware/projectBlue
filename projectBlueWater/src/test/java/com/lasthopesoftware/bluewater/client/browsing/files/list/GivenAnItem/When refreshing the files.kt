package com.lasthopesoftware.bluewater.client.browsing.files.list.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When refreshing the files` {

	companion object {
		private const val libraryId = 477
		private const val itemId = 294
	}

	private val expectedFiles = listOf(
        ServiceFile(0),
        ServiceFile(541),
        ServiceFile(878)
	)

	private val mut by lazy {
		val deferredFiles = DeferredPromise(expectedFiles)

		val itemProvider = mockk<ProvideItemFiles> {
            every {
                promiseFiles(
                    LibraryId(libraryId),
                    ItemId(itemId),
                    FileListParameters.Options.None
                )
            } returns listOf(
                ServiceFile(278),
                ServiceFile(145),
                ServiceFile(382),
                ServiceFile(561),
                ServiceFile(529),
            ).toPromise() andThen deferredFiles
        }

		val storedItemAccess = mockk<AccessStoredItems> {
            every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
        }

		Pair(deferredFiles, FileListViewModel(
            itemProvider,
            storedItemAccess,
        )
        )
	}

	private var isLoadingAfterReload = false

	@BeforeAll
	fun act() {
		val (deferredFiles, vm) = mut
		vm.loadItem(LibraryId(libraryId), Item(itemId, "king")).toExpiringFuture().get()

		val secondFuture = vm.promiseRefresh().toExpiringFuture()

		isLoadingAfterReload = vm.isLoading.value

		deferredFiles.resolve()

		secondFuture.get()
	}

	@Test
	fun `then the view model does reflect loading when refreshing`() {
		assertThat(isLoadingAfterReload).isTrue
	}

	@Test
	fun `then the item is not marked for sync`() {
		assertThat(mut.second.isSynced.value).isFalse
	}

	@Test
	fun `then the item value is correct`() {
		assertThat(mut.second.itemValue.value).isEqualTo("king")
	}

	@Test
	fun `then is loaded is correct`() {
		assertThat(mut.second.isLoading.value).isFalse
	}

	@Test
	fun thenTheLoadedFilesAreCorrect() {
		assertThat(mut.second.files.value).hasSameElementsAs(expectedFiles)
	}
}
