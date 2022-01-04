package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.namehillsoftware.handoff.promises.Promise

interface GetMediaItemsFromServiceFiles {
	fun promiseMediaItem(serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem>
	fun promiseMediaItemWithImage(serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem>
}
