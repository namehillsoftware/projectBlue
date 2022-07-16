package com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers

import androidx.lifecycle.ViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemListMenuViewModel(receiveTypedMessages: RegisterForTypedMessages<ItemListMenuMessage>) : ViewModel(), (ItemListMenuMessage) -> Unit {

	@Volatile
	private var shownMenuCount = 0

	private val onAnyMenuShown = receiveTypedMessages.registerReceiver(this)

	private val mutableIsAnyMenuShown = MutableStateFlow(false)

	val isAnyMenuShown = mutableIsAnyMenuShown.asStateFlow()

	override fun invoke(message: ItemListMenuMessage) {
		when (message) {
			is ItemListMenuMessage.MenuShown -> ++shownMenuCount
			is ItemListMenuMessage.MenuHidden -> shownMenuCount = (shownMenuCount - 1).coerceAtLeast(0)
		}

		mutableIsAnyMenuShown.value = shownMenuCount > 0
	}

	override fun onCleared() {
		onAnyMenuShown.close()
	}
}
