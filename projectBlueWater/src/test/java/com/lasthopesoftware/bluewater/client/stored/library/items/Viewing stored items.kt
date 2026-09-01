package com.lasthopesoftware.bluewater.client.stored.library.items

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.RecordingApplicationMessageBus
import com.lasthopesoftware.resources.strings.FakeStringResources
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class `Viewing stored items` {

	@Nested
	inner class `Given stored items` {

		@Nested
		inner class `When loading the items` {

			private val libraryId = 954

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

			private val mut by lazy {
				val applicationMessageBus = RecordingApplicationMessageBus()
				val storedItemAccess = StateChangeBroadcastingStoredItemAccess(
					FakeStoredItemAccess(*storedItems),
					applicationMessageBus,
				)
				Pair(
					storedItemAccess,
					StoredItemsListViewModel(
						storedItemAccess,
						FakeStringResources(syncedItems = "nh3Y3jZhNn"),
						applicationMessageBus,
					),
				)
			}

			@BeforeAll
			fun act() {
				val (items, vm) = mut
				vm.loadItem(LibraryId(libraryId)).toExpiringFuture().get()
				items.toggleSync(LibraryId(753), ItemId("148"), true)
				items.toggleSync(LibraryId(libraryId), Item("148", "R3lkZfN"), true)
				items.toggleSync(LibraryId(libraryId), ItemId("WrQ2hUO7"), false)
			}

			@Test
			fun `then the loaded library id is correct`() {
				assertThat(mut.second.loadedLibraryId).isEqualTo(LibraryId(libraryId))
			}

			@Test
			fun `then the loaded item is correct`() {
				assertThat(mut.second.loadedItem).isNull()
			}

			@Test
			fun `then the item name is correct`() {
				assertThat(mut.second.itemValue.value).isEqualTo("nh3Y3jZhNn")
			}

			@Test
			fun `then only expected items are loaded`() {
				assertThat(mut.second.items.value)
					.isEqualTo(listOf(Playlist("o2ivG3Jec", "Urnaper"), Item("148", "R3lkZfN")))
			}
		}
	}
}
