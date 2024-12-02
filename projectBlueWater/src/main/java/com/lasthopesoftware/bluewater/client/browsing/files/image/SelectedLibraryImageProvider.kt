package com.lasthopesoftware.bluewater.client.browsing.files.image

import android.graphics.Bitmap
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedLibraryImageProvider(private val selectedLibraryId: ProvideSelectedLibraryId, private val provideLibraryImages: ProvideLibraryImages) : ProvideScopedImages {
	override fun promiseFileBitmap(serviceFile: ServiceFile): Promise<Bitmap?> =
		Promise.Proxy {
			selectedLibraryId
				.promiseSelectedLibraryId()
				.eventually { libraryId ->
					libraryId
						?.let { l -> provideLibraryImages.promiseFileBitmap(l, serviceFile) }
						.keepPromise()
				}
		}
}
