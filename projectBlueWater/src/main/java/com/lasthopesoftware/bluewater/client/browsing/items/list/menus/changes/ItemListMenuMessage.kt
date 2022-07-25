package com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.HiddenListItemMenu
import com.lasthopesoftware.bluewater.shared.messages.TypedMessage

interface ItemListMenuMessage : TypedMessage {
	val menuItem: HiddenListItemMenu

	class MenuShown(override val menuItem: HiddenListItemMenu) : ItemListMenuMessage
	class MenuHidden(override val menuItem: HiddenListItemMenu) : ItemListMenuMessage
}
