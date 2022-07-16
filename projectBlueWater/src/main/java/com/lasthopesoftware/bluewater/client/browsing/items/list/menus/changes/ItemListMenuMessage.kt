package com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes

import com.lasthopesoftware.bluewater.shared.messages.TypedMessage

interface ItemListMenuMessage : TypedMessage {
	object MenuShown : ItemListMenuMessage
	object MenuHidden : ItemListMenuMessage
}
