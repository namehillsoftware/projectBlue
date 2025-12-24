package com.lasthopesoftware.bluewater.client.browsing.items.list.menus

import com.lasthopesoftware.observables.InteractionState

interface HiddenListItemMenu {
	val isMenuShown: InteractionState<Boolean>

	fun showMenu(): Boolean
	fun hideMenu(): Boolean
}
