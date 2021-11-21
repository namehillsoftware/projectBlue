package com.lasthopesoftware.bluewater.client.browsing

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat

class ProjectBlueBrowserService : MediaBrowserServiceCompat() {
	override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
		TODO("Not yet implemented")
	}

	override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
		TODO("Not yet implemented")
	}
}
