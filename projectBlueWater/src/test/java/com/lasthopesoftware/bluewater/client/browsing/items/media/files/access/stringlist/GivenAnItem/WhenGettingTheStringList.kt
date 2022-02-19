package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideFreshItems
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ProvideFileStringListsForParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private val stringList by lazy {
	val firstDeferredFilePromise = DeferredPromise("6Vbe")
	val secondDeferredFilePromise = DeferredPromise("BfCs02")
	val deferredItemPromise = DeferredPromise(emptyList<Item>())
	val itemProvider = mockk<ProvideFreshItems>().apply {
		var itemCalls = 0
		every { promiseItems(LibraryId(14), Item(32)) } answers {
			firstDeferredFilePromise.resolve()

			itemCalls++
			when (itemCalls) {
				1 -> emptyList<Item>().toPromise()
				else -> {
					secondDeferredFilePromise.resolve()
					deferredItemPromise
				}
			}
		}
	}

	val fileStringListProvider = mockk<ProvideFileStringListsForParameters>().apply {
		var fileCalls = 0
		every { promiseFileStringList(LibraryId(14), FileListParameters.Options.None, "Browse/Files", "ID=32", "Version=2") } answers {
			fileCalls++
			when (fileCalls) {
				1 -> {
					deferredItemPromise.resolve()
					firstDeferredFilePromise
				}
				else -> secondDeferredFilePromise
			}
		}
	}

	val itemStringListProvider = ItemStringListProvider(itemProvider, FileListParameters.getInstance(), fileStringListProvider)
	itemStringListProvider.promiseFileStringList(LibraryId(14), Item(32), FileListParameters.Options.None)
		.toFuture()
		.get()
}

class WhenGettingTheStringList {

	@Test fun thenTheStringListIsCorrectAfterCallingItemsAndStringListTwiceInCorrectOrder() {
		assertThat(stringList).isEqualTo("BfCs02")
	}
}
