package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import com.lasthopesoftware.bluewater.shared.AbstractIntKeyStringValue

class Playlist : AbstractIntKeyStringValue, IItem {
    constructor() : super()
    constructor(key: String) {
        this.key = key
    }
}
