package com.lasthopesoftware.bluewater.client.browsing.library.repository

interface StoredNowPlayingValues {
	val isRepeating: Boolean
	val nowPlayingId: Int
	val nowPlayingProgress: Long
	val savedTracksString: String?
}
