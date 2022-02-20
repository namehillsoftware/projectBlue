package com.lasthopesoftware.bluewater.client.browsing.items

import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue

open class Item : AbstractIntKeyStringValue, IItem {
	constructor(key: Int, value: String?, playlistId: Int) : super(key, value) {
		this.playlistId = PlaylistId(playlistId)
	}
    constructor(key: Int, value: String?) : super(key, value)
    constructor(key: Int) : super(key, null)

    constructor() : super()

    override fun hashCode(): Int {
        return key
    }

    var playlistId: PlaylistId? = null
        private set

	val itemId
		get() = ItemId(key)
}
