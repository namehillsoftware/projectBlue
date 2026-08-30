package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ItemUpdated
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ItemUpdatedListener {
	@Nested
	inner class `Given an updated item` {
		@Nested
		inner class `When handling` {

			val libraryId = 386

			private val mut by lazy {
				val fakeItemAccess = FakeStoredItemAccess(
					StoredItem(libraryId, "bUWhmGJ", StoredItem.ItemType.ITEM, "Maecenasipsum")
				)
				Pair(fakeItemAccess, StoredItemUpdatedListener(fakeItemAccess))
			}

			@BeforeAll
			fun act() {
				val (_, listener) = mut
				listener(ItemUpdated(LibraryId(libraryId), Item("bUWhmGJ", "zYzuRJWze5l")))
			}

			@Test
			fun `then the item name is updated`() {
				assertThat(mut.first.inMemoryStoredItems).isEqualTo(
					listOf(
						StoredItem(libraryId, "bUWhmGJ", StoredItem.ItemType.ITEM, "zYzuRJWze5l")
					)
				)
			}
		}
	}
}

