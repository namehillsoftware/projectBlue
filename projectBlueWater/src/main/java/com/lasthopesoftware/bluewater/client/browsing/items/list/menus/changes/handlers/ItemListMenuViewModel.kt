package com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class ItemListMenuViewModel(receiveTypedMessages: RegisterForTypedMessages<ItemListMenuMessage>) : ViewModel() {

	private val shownMenus = ConcurrentHashMap<HiddenListItemMenu, Unit>()

	private val onMenuShown = receiveTypedMessages.registerReceiver { message: ItemListMenuMessage.MenuShown ->
		shownMenus[message.menuItem] = Unit
		mutableIsAnyMenuShown.value = shownMenus.any()
	}

	private val onMenuHidden = receiveTypedMessages.registerReceiver { message: ItemListMenuMessage.MenuHidden ->
		shownMenus.remove(message.menuItem)
		mutableIsAnyMenuShown.value = shownMenus.any()
	}

	private val mutableIsAnyMenuShown = MutableStateFlow(false)

	val isAnyMenuShown = mutableIsAnyMenuShown.asStateFlow()

	fun hideAllMenus(): Boolean =
		shownMenus.keys.fold(false) { isAnyHidden, menu -> isAnyHidden or menu.hideMenu() }

	override fun onCleared() {
		shownMenus.clear()
		onMenuShown.close()
		onMenuHidden.close()
	}
}
