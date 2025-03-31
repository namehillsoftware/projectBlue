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

class `When loading a different file` {

	companion object {
		private const val libraryId = 525
		private const val firstItemId = "932"
		private const val secondItemId = "716"
	}

	private val expectedFiles = listOf(
        ServiceFile("96"),
        ServiceFile("768"),
        ServiceFile("505"),
        ServiceFile("90"),
        ServiceFile("219"),
        ServiceFile("11"),
	)

	private val mut by lazy {
		val deferredFiles = DeferredPromise(expectedFiles)

		val itemProvider = mockk<ProvideItemFiles> {
            every {
                promiseFiles(
                    LibraryId(libraryId),
                    ItemId(firstItemId),
                    FileListParameters.Options.None
                )
            } returns listOf(
                ServiceFile("278"),
                ServiceFile("145"),
                ServiceFile("382"),
                ServiceFile("561"),
                ServiceFile("529"),
            ).toPromise()

			every {
				promiseFiles(
					LibraryId(libraryId),
					ItemId(secondItemId),
					FileListParameters.Options.None
				)
			} returns deferredFiles
        }

		val storedItemAccess = mockk<AccessStoredItems> {
            every { isItemMarkedForSync(any(), any<Item>()) } returns false.toPromise()
        }

		Pair(
			deferredFiles,
			FileListViewModel(
				itemProvider,
				storedItemAccess,
			)
		)
	}

	private var isLoadingAfterReload = false

	@BeforeAll
	fun act() {
		val (deferredFiles, vm) = mut
		vm.loadItem(LibraryId(libraryId), Item(firstItemId, "king")).toExpiringFuture().get()

		val secondFuture = vm.loadItem(LibraryId(libraryId), Item(secondItemId, "king")).toExpiringFuture()

		isLoadingAfterReload = vm.isLoading.value

		deferredFiles.resolve()

		secondFuture.get()
	}

	@Test
	fun `then the view model does reflect loading a new item`() {
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
