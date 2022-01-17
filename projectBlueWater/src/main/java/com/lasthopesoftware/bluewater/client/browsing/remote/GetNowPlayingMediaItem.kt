package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.namehillsoftware.handoff.promises.Promise

interface GetNowPlayingMediaItem {
	fun promiseNowPlayingItem(): Promise<MediaBrowserCompat.MediaItem?>
}
