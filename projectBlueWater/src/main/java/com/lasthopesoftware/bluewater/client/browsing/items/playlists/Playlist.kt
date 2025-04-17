package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import android.os.Parcelable
import com.lasthopesoftware.bluewater.client.browsing.items.IItem
import kotlinx.parcelize.Parcelize

@Parcelize
data class Playlist(override val key: String, override val value: String? = null) :  IItem, Parcelable
