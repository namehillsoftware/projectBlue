package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WhenSyncingAChildItem {

	companion object {
		private const val libraryId = 374
		private const val itemId = "178"
		private const val itemValue = "reply"
	}

	private val viewModel by lazy {
		val storedItemAccess = FakeStoredItemAccess()
		ReusableChildItemViewModel(
			storedItemAccess,
			RecordingTypedMessageBus()
		)
	}

	@BeforeAll
	fun act() {
		viewModel.update(LibraryId(libraryId), Item(itemId, itemValue))
		viewModel.toggleSync().toExpiringFuture().get()
	}

	@Test
	fun `then the item is synced`() {
		assertThat(viewModel.isSynced.value).isTrue
	}
}
