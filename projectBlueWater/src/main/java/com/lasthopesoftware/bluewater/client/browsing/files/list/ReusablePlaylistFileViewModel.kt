package com.lasthopesoftware.bluewater.client.browsing.files.list

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.messages.SendTypedMessages
import com.lasthopesoftware.observables.MutableInteractionState
import com.lasthopesoftware.resources.closables.ResettableCloseable
import com.namehillsoftware.handoff.promises.Promise

class ReusablePlaylistFileViewModel(
	private val sendItemMenuMessages: SendTypedMessages<ItemListMenuMessage>,
	private val viewFileItem: ViewFileItem
) : ViewPlaylistFileItem, HiddenListItemMenu, ResettableCloseable {

	private val mutableIsMenuShown = MutableInteractionState(false)

	override val artist = viewFileItem.artist
	override val title = viewFileItem.title
	override val isMenuShown = mutableIsMenuShown.asInteractionState()

	override fun promiseUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Unit> =
		viewFileItem.promiseUpdate(libraryId, serviceFile)

	override fun showMenu(): Boolean =
		mutableIsMenuShown.compareAndSet(expect = false, update = true)
			.also {
				if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuShown(this))
			}

	override fun hideMenu(): Boolean =
		mutableIsMenuShown.compareAndSet(expect = true, update = false)
			.also {
				if (it) sendItemMenuMessages.sendMessage(ItemListMenuMessage.MenuHidden(this))
			}

	override fun close() {
		viewFileItem.close()
		reset()
	}

	override fun reset() {
		viewFileItem.reset()
		resetState()
	}

	private fun resetState() {
		hideMenu()
	}
}
