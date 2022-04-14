package com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.client.stored.library.items.files.system.uri.MediaFileUriProvider
import com.lasthopesoftware.bluewater.client.stored.library.items.files.uri.StoredFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

open class BestMatchUriProvider(
    private val library: Library,
    private val storedFileUriProvider: StoredFileUriProvider,
    private val mediaFileUriProvider: MediaFileUriProvider,
    private val remoteFileUriProvider: RemoteFileUriProvider
) : IFileUriProvider {
    override fun promiseFileUri(serviceFile: ServiceFile): Promise<Uri> =
        storedFileUriProvider
            .promiseFileUri(serviceFile)
            .eventually { storedFileUri ->
				when {
					storedFileUri != null -> storedFileUri.toPromise()
					!library.isUsingExistingFiles -> remoteFileUriProvider.promiseFileUri(serviceFile)
					else -> mediaFileUriProvider
						.promiseFileUri(serviceFile)
						.eventually { mediaFileUri ->
							mediaFileUri?.toPromise() ?: remoteFileUriProvider.promiseFileUri(serviceFile)
						}
				}
			}
}
