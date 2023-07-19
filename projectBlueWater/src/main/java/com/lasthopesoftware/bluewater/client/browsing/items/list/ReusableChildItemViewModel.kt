package com.lasthopesoftware.bluewater.client.browsing.items.list

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.client.browsing.items.itemId
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.closables.ResettableCloseable
import com.namehillsoftware.handoff.promises.Promise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReusableChildItemViewModel(
	private val storedItemAccess: AccessStoredItems,
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
) : HiddenListItemMenu, ResettableCloseable {
	private var item: IItem? = null
	private var libraryId: LibraryId? = null

	private val mutableIsSynced = MutableStateFlow(false)
	private val mutableIsMenuShown = MutableStateFlow(false)

	val isSynced = mutableIsSynced.asStateFlow()
	override val isMenuShown = mutableIsMenuShown.asStateFlow()

	fun update(libraryId: LibraryId, item: IItem) {
		this.libraryId = libraryId
		this.item = item
	}

	fun toggleSync(): Promise<Unit> = libraryId
		?.let { l ->
			item?.run {
				storedItemAccess
					.toggleSync(l, itemId)
					.then { it -> mutableIsSynced.value = it && l == libraryId && this == item }
			}
		}
		.keepPromise(Unit)

	override fun showMenu(): Boolean {
		if (!mutableIsMenuShown.compareAndSet(expect = false, update = true)) return false

		libraryId
			?.let { l ->
				item?.run {
					storedItemAccess
						.isItemMarkedForSync(l, itemId)
						.then { it ->
							mutableIsSynced.value = it && l == libraryId && this == item
						}
				}
			}

		sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuShown(this))

		return true
	}

	override fun hideMenu(): Boolean =
		mutableIsMenuShown.compareAndSet(expect = true, update = false).also {
			if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuHidden(this))
		}

	override fun reset() {
		mutableIsSynced.value = false
		mutableIsMenuShown.value = false
	}

	override fun close() {
		reset()
	}
}
