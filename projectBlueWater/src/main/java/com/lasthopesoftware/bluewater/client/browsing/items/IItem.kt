package com.lasthopesoftware.bluewater.client.browsing.items

import com.lasthopesoftware.bluewater.shared.IIntKeyStringValue

interface IItem : IIntKeyStringValue

val IItem.itemId: KeyedIdentifier
	get() = (this as? Item)?.playlistId ?: ItemId(key)
