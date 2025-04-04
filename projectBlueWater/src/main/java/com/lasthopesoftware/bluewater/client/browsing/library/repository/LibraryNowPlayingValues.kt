package com.lasthopesoftware.bluewater.client.browsing.library.repository

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.IdentifiableEntity

@Keep
data class LibraryNowPlayingValues(
	override var id: Int = -1,
	override var isRepeating: Boolean = false,
	override var nowPlayingId: Int = -1,
	override var nowPlayingProgress: Long = -1,
	override var savedTracksString: String? = null
) : StoredNowPlayingValues, IdentifiableEntity
