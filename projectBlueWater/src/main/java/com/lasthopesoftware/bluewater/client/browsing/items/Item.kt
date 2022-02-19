package com.lasthopesoftware.bluewater.client.browsing.items

import com.lasthopesoftware.bluewater.client.browsing.items.playlists.Playlist
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue

open class Item : AbstractIntKeyStringValue, IItem {
    var playlistId: Int? = null

    constructor(key: Int, value: String?) : super(key, value)
    constructor(key: Int) : super() {
        this.key = key
    }

    constructor() : super()

    override fun hashCode(): Int {
        return key
    }

    val playlist: Playlist?
        get() = playlistId?.let(::Playlist)

	val itemId
		get() = ItemId(key)
}
