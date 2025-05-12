package com.lasthopesoftware.bluewater.client.browsing.library.repository

import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.IdentifiableEntity

@Keep
data class Library(
	override var id: Int = -1,
	var libraryName: String? = null,
	override var isRepeating: Boolean = false,
	override var nowPlayingId: Int = -1,
	override var nowPlayingProgress: Long = -1,
	override var savedTracksString: String? = null,
	var isUsingExistingFiles: Boolean = false,
	var serverType: String? = null,
	var syncedFileLocation: SyncedFileLocation? = null,
	var connectionSettings: String? = null,
) : IdentifiableEntity, StoredNowPlayingValues {

	@Keep
	enum class ServerType {
		MediaCenter
	}
}
