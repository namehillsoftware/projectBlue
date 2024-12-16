package com.lasthopesoftware.bluewater.client.browsing.remote

import android.support.v4.media.MediaBrowserCompat
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface GetMediaItemsFromServiceFiles {
	fun promiseMediaItem(libraryId: LibraryId, serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem>
	fun promiseMediaItemWithImage(libraryId: LibraryId, serviceFile: ServiceFile): Promise<MediaBrowserCompat.MediaItem>
}
