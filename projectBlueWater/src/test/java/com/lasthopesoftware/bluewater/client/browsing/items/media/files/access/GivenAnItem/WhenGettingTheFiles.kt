package com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.GivenAnItem

import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.ItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.access.stringlist.ProvideFileStringListForItem
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private val files by lazy {
	val provideFileStringList = mockk<ProvideFileStringListForItem>().apply {
		every { promiseFileStringList(LibraryId(510), ItemId(691), FileListParameters.Options.None) } returns Promise(
			"2;12;-1;941449;941450;941451;941452;941453;941454;941455;941456;941457;941458;941459;941460"
		)
	}

	val itemFileProvider = ItemFileProvider(provideFileStringList)
	itemFileProvider.promiseFiles(LibraryId(510), ItemId(691), FileListParameters.Options.None).toExpiringFuture().get()
}

class WhenGettingTheFiles {
	@Test fun `then the service files are correct`() {
		assertThat(files)
			.isEqualTo(listOf(941449, 941450, 941451, 941452, 941453, 941454, 941455, 941456, 941457, 941458, 941459, 941460).map(::ServiceFile))
	}
}
