package com.lasthopesoftware.bluewater.client.browsing.items

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface LocalItem : IItem

@Parcelize
data class StoredLocalItem(override val value: String) : LocalItem, Parcelable {
	companion object {
		val id = "de679e3d-75c1-4ba5-9ef1-99a848b4152c"
	}

	@IgnoredOnParcel
	override val key = id
	@IgnoredOnParcel
	override val itemId = ItemId(key)
}
