package com.lasthopesoftware.bluewater.client.browsing.items.playlists

import android.os.Parcelable
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class PlaylistId(override val id: String) : KeyedIdentifier, Parcelable
