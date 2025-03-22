package com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.GivenAnItem

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheStringList {

	private val stringList by lazy {
		val itemStringListProvider = ItemStringListProvider(
			mockk {
				every { promiseLibraryConnection(LibraryId(14)) } returns Promise(mockk<LiveServerConnection> {
					every { dataAccess } returns mockk<RemoteLibraryAccess> {
						every { promiseFileStringList(ItemId(32)) } returns "BfCs02".toPromise()
					}
				})
			}
		)
		itemStringListProvider.promiseFileStringList(LibraryId(14), ItemId(32), FileListParameters.Options.None)
			.toExpiringFuture()
			.get()
	}

	@Test
	fun `then the string list is correct`() {
		assertThat(stringList).isEqualTo("BfCs02")
	}
}
