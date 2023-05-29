package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLibraryImages {
	fun promiseFileBitmap(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Bitmap?>
}
