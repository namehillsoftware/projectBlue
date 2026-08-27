package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `Viewing synced items` {

	@Nested
	inner class `Given stored items` {

		@Nested
		inner class `When loading the items` {

			private val libraryId = 954

			private val expectedStoredItems by lazy {
				listOf(
					StoredItem(
						libraryId = libraryId,
						serviceId = "WrQ2hUO7",
						itemType = StoredItem.ItemType.ITEM,
						itemName = "Nasceturiaculis",
					),
					StoredItem(
						libraryId = libraryId,
						serviceId = "o2ivG3Jec",
						itemType = StoredItem.ItemType.PLAYLIST,
						itemName = "Urnaper",
					),
				)
			}

			private val storedItems by lazy {
				arrayOf(
					StoredItem(
						libraryId = libraryId,
						serviceId = "b551CKejE",
						itemType = StoredItem.ItemType.FILE,
						itemName = "Sapienproin",
					),
					StoredItem(
						libraryId = 547,
						serviceId = "NP4cPHHdBx",
						itemType = StoredItem.ItemType.ITEM,
						itemName = "Nasceturiaculis",
					),
				) + expectedStoredItems
			}

			private val mut by lazy {
				SyncedItemsListViewModel(FakeStoredItemAccess(*storedItems))
			}

			@BeforeAll
			fun act() {
				mut.loadItems(LibraryId(libraryId)).toExpiringFuture().get()
			}

			@Test
			fun `then only expected items are loaded`() {
				assertThat(mut.storedItems.value).isEqualTo(expectedStoredItems)
			}
		}
	}
}
