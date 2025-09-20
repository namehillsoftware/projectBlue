package com.lasthopesoftware.bluewater.client.browsing.items.list.menu.GivenMenus

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.shared.observables.MutableInteractionState

class FakeMenu : HiddenListItemMenu {
	private val mutableIsMenuShown = MutableInteractionState(false)

	override val isMenuShown = mutableIsMenuShown.asInteractionState()

	override fun showMenu(): Boolean {
		mutableIsMenuShown.value = true
		return true
	}

	override fun hideMenu(): Boolean {
		mutableIsMenuShown.value = false
		return true
	}
}
