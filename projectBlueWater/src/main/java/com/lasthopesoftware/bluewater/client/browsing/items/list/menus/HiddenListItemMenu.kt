package com.lasthopesoftware.bluewater.client.browsing.items.list.menus

import kotlinx.coroutines.flow.StateFlow

interface HiddenListItemMenu {
	val isMenuShown: StateFlow<Boolean>

	fun showMenu(): Boolean
	fun hideMenu(): Boolean
}
