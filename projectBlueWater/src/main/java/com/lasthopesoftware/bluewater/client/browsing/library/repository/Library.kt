package com.lasthopesoftware.bluewater.client.browsing.library.repository

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.IdentifiableEntity

@Keep
data class Library(
	override var id: Int = -1,
	var libraryName: String? = null,
	var isRepeating: Boolean = false,
	var nowPlayingId: Int = -1,
	var nowPlayingProgress: Long = -1,
	var savedTracksString: String? = null,
	var isUsingExistingFiles: Boolean = false,
	var serverType: ServerType? = null,
	var connectionSettings: String? = null,
) : IdentifiableEntity {

	@Keep
	enum class ServerType {
		MediaCenter
	}
}
