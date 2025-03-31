package com.lasthopesoftware.bluewater.client.browsing.items.playlists.GivenANestedPlaylist

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistItemFinder
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.views.KnownViews
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import java.util.Random

class WhenFindingThePlaylistItem {
	companion object {
		private val random = Random()

		private fun setupItemProviderWithItems(itemProvider: ProvideItems, sourceItem: ItemId, numberOfChildren: Int, withPlaylistIds: Boolean): List<Item> {
			val items = ArrayList<Item>(numberOfChildren)
			for (i in 0 until numberOfChildren) {
				val newItem = if (withPlaylistIds) Item(random.nextInt().toString(), null, PlaylistId(random.nextInt().toString()))
				else Item(random.nextInt().toString())
				items.add(newItem)
			}
			every { itemProvider.promiseItems(LibraryId(3), sourceItem) } returns Promise(items)
			return items
		}
	}

	private val playlistId by lazy { random.nextInt().toString() }

	private val expectedItem by lazy { Item(random.nextInt().toString(), null, PlaylistId(playlistId)) }

	private val item by lazy {
		val itemProvider = mockk<ProvideItems>()
		every { itemProvider.promiseItems(any(), any()) } returns Promise(emptyList())
		every { itemProvider.promiseItems(LibraryId(3)) } returns Promise(
			listOf(
				Item("2", "Music"),
				Item("16", KnownViews.Playlists)
			)
		)

		setupItemProviderWithItems(
			itemProvider,
			ItemId("2"),
			3,
			false
		)
		var generatedItems = setupItemProviderWithItems(
			itemProvider,
			ItemId("16"),
			15,
			true
		)
		val firstLevelChosenItem = generatedItems[random.nextInt(generatedItems.size)]
		for (item in generatedItems) {
			if (item == firstLevelChosenItem) continue
			setupItemProviderWithItems(
				itemProvider,
				item.itemId,
				100,
				true
			)
		}
		generatedItems = setupItemProviderWithItems(
			itemProvider,
			ItemId("16"),
			90,
			true
		)

		val secondLevelChosenItem = generatedItems[random.nextInt(generatedItems.size)]
		for (item in generatedItems) {
			if (item == secondLevelChosenItem) continue
			every { itemProvider.promiseItems(LibraryId(3), item.itemId) } returns Promise(emptyList())
		}
		val decoy = Item(random.nextInt().toString(), null, PlaylistId(random.nextInt().toString()))

		every { itemProvider.promiseItems(LibraryId(3), secondLevelChosenItem.itemId) } returns Promise(
			listOf(
				decoy,
				expectedItem
			)
		)

		val playlistItemFinder = PlaylistItemFinder(itemProvider)
		playlistItemFinder.promiseItem(LibraryId(3), Playlist(playlistId)).toExpiringFuture().get()
	}

	@Test
	fun thenTheReturnedItemIsTheExpectedItem() {
		assertThat(item).isEqualTo(expectedItem)
	}
}
