package com.lasthopesoftware.bluewater.client.browsing.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.caching.uri.CachedAudioFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.external.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class BestMatchUriProvider(
    private val libraryProvider: ILibraryProvider,
    private val storedFileUriProvider: StoredFileUriProvider,
    private val cachedAudioFileUriProvider: CachedAudioFileUriProvider,
    private val mediaFileUriProvider: MediaFileUriProvider,
    private val remoteFileUriProvider: RemoteFileUriProvider
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
								!library.isUsingExistingFiles -> cachedOrRemoteUri(libraryId, serviceFile)
								else -> mediaFileUriProvider
									.promiseUri(libraryId, serviceFile)
									.eventually { mediaFileUri ->
										mediaFileUri?.toPromise() ?: cachedOrRemoteUri(libraryId, serviceFile)
									}
							}
						}
			}

	private fun cachedOrRemoteUri(libraryId: LibraryId, serviceFile: ServiceFile) =
		cachedAudioFileUriProvider.promiseUri(libraryId, serviceFile)
			.eventually {
				it?.toPromise() ?: remoteFileUriProvider.promiseUri(libraryId, serviceFile)
			}
}
