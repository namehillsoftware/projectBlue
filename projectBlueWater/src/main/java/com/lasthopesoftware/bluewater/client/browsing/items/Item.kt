package com.lasthopesoftware.bluewater.client.browsing.items

import android.os.Parcelable
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistId
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Item(override var key: String, override var value: String? = null, val playlistId: PlaylistId? = null) :  IItem, Parcelable {
	@IgnoredOnParcel
	override val itemId = ItemId(key)
}
