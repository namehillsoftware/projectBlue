package com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers

import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import java.util.concurrent.ConcurrentHashMap

class ItemListMenuViewModel(receiveTypedMessages: RegisterForTypedMessages<ItemListMenuMessage>)
	: LifecycleEventObserver, OnBackPressedCallback(false) {

	private val shownMenus = ConcurrentHashMap<HiddenListItemMenu, Unit>()

	private val onMenuShown = receiveTypedMessages.registerReceiver { message: ItemListMenuMessage.MenuShown ->
		shownMenus[message.menuItem] = Unit
		isEnabled = shownMenus.any()
	}

	private val onMenuHidden = receiveTypedMessages.registerReceiver { message: ItemListMenuMessage.MenuHidden ->
		shownMenus.remove(message.menuItem)
		isEnabled = shownMenus.any()
	}

	fun hideAllMenus(): Boolean =
		shownMenus.keys.fold(false) { isAnyHidden, menu -> isAnyHidden or menu.hideMenu() }

	override fun handleOnBackPressed() {
		hideAllMenus()
	}

	override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
		if (event != Lifecycle.Event.ON_DESTROY) return

		shownMenus.clear()
		onMenuShown.close()
		onMenuHidden.close()
	}
}
