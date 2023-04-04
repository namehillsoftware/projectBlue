package com.lasthopesoftware.bluewater.client.browsing.items.list.AndItHasChildItems.AndAChildItemIsSynced

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.resources.RecordingTypedMessageBus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 391
private const val childItemId = 637

class WhenShowingTheItemMenu {

	private val recordingMessageBus = RecordingTypedMessageBus<ItemListMenuMessage>()

	private val viewModel by lazy {
		val storedItemAccess = FakeStoredItemAccess(StoredItem(libraryId, childItemId, StoredItem.ItemType.ITEM))

		ReusableChildItemViewModel(
			storedItemAccess,
			recordingMessageBus,
		)
	}

	@BeforeAll
	fun act() {
		viewModel.update(LibraryId(libraryId), Item(childItemId, "leaf"))
		viewModel.showMenu()
	}

	@Test
	fun `then a menu shown message is sent`() {
		assertThat(recordingMessageBus.recordedMessages.filterIsInstance<ItemListMenuMessage.MenuShown>()
				.map { it.menuItem }).containsOnlyOnce(viewModel)
	}

	@Test
	fun `then the child item is marked for sync`() {
		assertThat(viewModel.isSynced.value).isTrue
	}

	@Test
	fun `then the menu is shown`() {
		assertThat(viewModel.isMenuShown.value).isTrue
	}
}
