package com.lasthopesoftware.bluewater.client.browsing.items.playlists.GivenANestedPlaylist

import com.lasthopesoftware.bluewater.client.browsing.items.Item
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistItemFinder
import com.lasthopesoftware.bluewater.client.browsing.library.views.PlaylistViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.StandardViewItem
import com.lasthopesoftware.bluewater.client.browsing.library.views.access.ProvideLibraryViews
import com.lasthopesoftware.bluewater.shared.promises.extensions.toFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Test
import java.util.*

class WhenFindingThePlaylistItem {
	companion object {
		private val random = Random()

		private val playlistId by lazy { random.nextInt() }

		private val expectedItem by lazy { Item(random.nextInt()).setPlaylistId(playlistId) }

		private val item by lazy {
			val libraryViews = mockk<ProvideLibraryViews>()
			every { libraryViews.promiseLibraryViews() } returns Promise(
				listOf(
					StandardViewItem(2, "Music"),
					PlaylistViewItem(16)
				)
			)

			val itemProvider = mockk<ProvideItems>()
			every { itemProvider.promiseItems(any()) } returns Promise(emptyList())

			setupItemProviderWithItems(
				itemProvider,
				2,
				3,
				false
			)
			var generatedItems = setupItemProviderWithItems(
				itemProvider,
				16,
				15,
				true
			)
			val firstLevelChosenItem = generatedItems[random.nextInt(generatedItems.size)]
			for (item in generatedItems) {
				if (item == firstLevelChosenItem) continue
				setupItemProviderWithItems(
					itemProvider,
					item.key,
					100,
					true
				)
			}
			generatedItems = setupItemProviderWithItems(
				itemProvider,
				16,
				90,
				true
			)

			val secondLevelChosenItem = generatedItems[random.nextInt(generatedItems.size)]
			for (item in generatedItems) {
				if (item == secondLevelChosenItem) continue
				every { itemProvider.promiseItems(item.key) } returns Promise(emptyList())
			}
			val decoy = Item(random.nextInt()).setPlaylistId(random.nextInt())

			every { itemProvider.promiseItems(secondLevelChosenItem.key) } returns Promise(
				listOf(
					decoy,
					expectedItem
				)
			)

			val playlistItemFinder = PlaylistItemFinder(libraryViews, itemProvider)
			playlistItemFinder.promiseItem(Playlist(playlistId)).toFuture().get()
		}

		private fun setupItemProviderWithItems(itemProvider: ProvideItems, sourceItem: Int, numberOfChildren: Int, withPlaylistIds: Boolean): List<Item> {
			val items = ArrayList<Item>(numberOfChildren)
			for (i in 0 until numberOfChildren) {
				val newItem = Item(random.nextInt())
				if (withPlaylistIds) newItem.playlistId = random.nextInt()
				items.add(newItem)
			}
			every { itemProvider.promiseItems(sourceItem) } returns Promise(items)
			return items
		}
	}

	@Test
	fun thenTheReturnedItemIsTheExpectedItem() {
		assertThat(item).isEqualTo(expectedItem)
	}
}