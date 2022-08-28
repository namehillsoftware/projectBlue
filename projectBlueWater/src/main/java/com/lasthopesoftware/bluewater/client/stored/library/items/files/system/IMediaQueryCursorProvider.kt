package com.lasthopesoftware.bluewater.client.stored.library.items.files.system

import android.database.Cursor
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

/**
 * Created by david on 6/13/16.
 */
interface IMediaQueryCursorProvider {
	fun getMediaQueryCursor(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Cursor?>
}
