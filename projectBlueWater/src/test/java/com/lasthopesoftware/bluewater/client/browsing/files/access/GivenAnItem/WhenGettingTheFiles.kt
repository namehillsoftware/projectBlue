package com.lasthopesoftware.bluewater.client.browsing.files.access.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WhenGettingTheFiles {
	private val files by lazy {
		val provideFileStringList = mockk<ProvideFileStringListForItem>().apply {
			every { promiseFileStringList(LibraryId(510), ItemId("691")) } returns Promise(
				"2;12;-1;941449;941450;941451;941452;941453;941454;941455;941456;941457;941458;941459;941460"
			)
		}

		val itemFileProvider = ItemFileProvider(provideFileStringList)
		itemFileProvider.promiseFiles(LibraryId(510), ItemId("691")).toExpiringFuture().get()
	}

	@Test
	fun `then the service files are correct`() {
		assertThat(files)
			.isEqualTo(listOf("941449", "941450", "941451", "941452", "941453", "941454", "941455", "941456", "941457", "941458", "941459", "941460").map(::ServiceFile))
	}
}
