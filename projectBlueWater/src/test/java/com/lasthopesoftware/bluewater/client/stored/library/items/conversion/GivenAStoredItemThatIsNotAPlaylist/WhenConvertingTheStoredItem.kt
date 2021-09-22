package com.lasthopesoftware.bluewater.client.stored.library.items.conversion.GivenAStoredItemThatIsNotAPlaylist

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.FindPlaylistItem
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
	fun thenItemTypeIsTheCorrectItemType() {
		assertThat(convertedItem!!.itemType).isEqualTo(ItemType.ITEM)
	}

	@Test
	fun thenTheItemHasTheOriginalId() {
		assertThat(convertedItem!!.serviceId).isEqualTo(17)
	}

	@Test
	fun thenTheOriginalItemIsMarkedForSync() {
		assertThat(
			FuturePromise(
				storedItemAccess.isItemMarkedForSync(
					LibraryId(12),
					Item(17)
				)
			).get()
		).isTrue
	}

	companion object {
		private var convertedItem: StoredItem? = null
		private val storedItemAccess = FakeStoredItemAccess(
			StoredItem(1, 17, ItemType.ITEM)
		)

		@BeforeClass
		@JvmStatic
		fun before() {
			val storedItem = StoredItem(1, 17, ItemType.ITEM)
			val playlistItem = mockk<FindPlaylistItem>()
			every { playlistItem.promiseItem(any()) } returns Promise.empty()
			every { playlistItem.promiseItem(match { p -> p.key == 17 }) } returns Promise(Item(34))
			val playlistItemsConverter = StoredPlaylistItemsConverter(
				{ Promise(Library().setId(12)) },
				playlistItem,
				storedItemAccess
			)
			convertedItem =
				FuturePromise(playlistItemsConverter.promiseConvertedStoredItem(storedItem)).get()
		}
	}
}
