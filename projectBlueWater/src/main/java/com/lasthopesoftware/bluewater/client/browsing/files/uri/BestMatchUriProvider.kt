package com.lasthopesoftware.bluewater.client.browsing.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ProvideLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class BestMatchUriProvider(
    private val libraryProvider: ProvideLibraries,
    private val storedFileUriProvider: StoredFileUriProvider,
	private val mediaFileUriProvider: MediaFileUriProvider,
    private val remoteFileUriProvider: ProvideFileUrisForLibrary,
) : ProvideFileUrisForLibrary {
    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
        storedFileUriProvider
            .promiseUri(libraryId, serviceFile)
            .eventually { storedFileUri ->
				storedFileUri
					?.toPromise()
					?: libraryProvider
						.promiseLibrary(libraryId)
						.eventually { library ->
							when {
								library == null -> Promise.empty()
								!library.isUsingExistingFiles -> remoteFileUriProvider.promiseUri(libraryId, serviceFile)
								else -> mediaFileUriProvider
									.promiseUri(libraryId, serviceFile)
									.eventually { mediaFileUri ->
										mediaFileUri?.toPromise() ?: remoteFileUriProvider.promiseUri(libraryId, serviceFile)
									}
							}
						}
			}
}
