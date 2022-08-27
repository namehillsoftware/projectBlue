package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ProvideFileStringListsForParameters
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheStringList {

	private val stringList by lazy {

		val fileStringListProvider = mockk<ProvideFileStringListsForParameters>().apply {
			every { promiseFileStringList(LibraryId(14), FileListParameters.Options.None, "Browse/Files", "ID=32", "Version=2") } returns Promise("BfCs02")
		}

		val itemStringListProvider = ItemStringListProvider(FileListParameters, fileStringListProvider)
		itemStringListProvider.promiseFileStringList(LibraryId(14), ItemId(32), FileListParameters.Options.None)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the string list is correct`() {
		assertThat(stringList).isEqualTo("BfCs02")
	}
}
