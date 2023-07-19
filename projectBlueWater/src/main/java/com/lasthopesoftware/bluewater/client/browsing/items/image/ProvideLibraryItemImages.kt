package com.lasthopesoftware.bluewater.client.browsing.items.image

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.items.ItemId
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryItemImages {
	fun promiseImage(libraryId: LibraryId, itemId: ItemId): Promise<Bitmap?>
}
