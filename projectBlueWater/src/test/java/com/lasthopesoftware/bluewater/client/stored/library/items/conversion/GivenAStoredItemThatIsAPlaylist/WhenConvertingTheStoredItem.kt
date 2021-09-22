package com.lasthopesoftware.bluewater.client.stored.library.items.conversion.GivenAStoredItemThatIsAPlaylist

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.FindPlaylistItem
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.FakeStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem.ItemType
import com.lasthopesoftware.bluewater.client.stored.library.items.conversion.StoredPlaylistItemsConverter
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenConvertingTheStoredItem {
	@Test
	fun thenTheNewItemTypeIsTheCorrectItemType() {
		assertThat(convertedItem!!.itemType).isEqualTo(ItemType.ITEM)
	}

	@Test
	fun thenTheNewItemTypeHasTheCorrectId() {
		assertThat(convertedItem!!.serviceId).isEqualTo(34)
	}

	@Test
	fun thenTheConvertedItemIsMarkedForSync() {
		assertThat(
			FuturePromise(
				storedItemAccess.isItemMarkedForSync(
					LibraryId(14),
					Item(34)
				)
			).get()
		).isTrue
	}

	@Test
	fun thenTheOriginalItemIsNotMarkedForSync() {
		assertThat(
			FuturePromise(
				storedItemAccess.isItemMarkedForSync(
					LibraryId(14),
					Playlist(15)
				)
			).get()
		).isFalse
	}

	companion object {
		private var convertedItem: StoredItem? = null
		private val storedItemAccess = FakeStoredItemAccess(
			StoredItem(1, 15, ItemType.PLAYLIST)
		)

		@BeforeClass
		@JvmStatic
		fun before() {
			val storedItem = StoredItem()
			storedItem.serviceId = 15
			storedItem.itemType = ItemType.PLAYLIST
			val playlistItem = mockk<FindPlaylistItem>()
			every { playlistItem.promiseItem(any()) } returns Promise.empty()
			every { playlistItem.promiseItem(match { p -> p.key == 15 }) } returns Promise(Item(34))
			val playlistItemsConverter = StoredPlaylistItemsConverter(
				{ Promise(Library().setId(14)) },
				playlistItem,
				storedItemAccess
			)
			convertedItem =
				FuturePromise(playlistItemsConverter.promiseConvertedStoredItem(storedItem)).get()
		}
	}
}
