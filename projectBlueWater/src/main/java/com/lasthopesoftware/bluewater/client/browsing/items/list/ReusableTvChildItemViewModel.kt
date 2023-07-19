package com.lasthopesoftware.bluewater.client.browsing.items.list

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.items.image.ProvideLibraryItemImages
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.toPromise
import com.lasthopesoftware.resources.closables.ResettableCloseable
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReusableTvChildItemViewModel(
	private val libraryItemImages: ProvideLibraryItemImages
) : ResettableCloseable {
	private var item: IItem? = null
	private var libraryId: LibraryId? = null

	private var promisedItemImage = Unit.toPromise()

	private val mutableItemImage = MutableStateFlow<Bitmap?>(null)
	val itemImage = mutableItemImage.asStateFlow()

	@Synchronized
	fun update(updatedLibraryId: LibraryId, updatedItem: IItem) {
		this.libraryId = updatedLibraryId
		this.item = updatedItem

		promisedItemImage.cancel()

		promisedItemImage = Promise.Proxy { cp ->
			libraryItemImages
				.promiseImage(updatedLibraryId, ItemId(updatedItem.key))
				.also(cp::doCancel)
				.then { it, ct ->
					if (!ct.isCancelled && updatedLibraryId == libraryId && updatedItem == item)
						mutableItemImage.value = it
				}
		}
	}

	override fun reset() {
		promisedItemImage.cancel()
	}

	override fun close() {
		reset()
	}
}
