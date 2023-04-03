package com.lasthopesoftware.bluewater.client.browsing.items.list.menu.GivenMenus

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMenu : HiddenListItemMenu {
	private val mutableIsMenuShown = MutableStateFlow(false)

	override val isMenuShown = mutableIsMenuShown.asStateFlow()

	override fun showMenu(): Boolean {
		mutableIsMenuShown.value = true
		return true
	}

	override fun hideMenu(): Boolean {
		mutableIsMenuShown.value = false
		return true
	}
}
