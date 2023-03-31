package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import android.os.Parcelable
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaylistId(override val id: Int) : KeyedIdentifier, Parcelable
