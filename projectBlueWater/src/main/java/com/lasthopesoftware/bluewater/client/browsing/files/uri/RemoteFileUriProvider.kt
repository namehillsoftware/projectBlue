package com.lasthopesoftware.bluewater.client.browsing.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.IServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.promises.Promise

private val logger by lazyLogger<RemoteFileUriProvider>()

class RemoteFileUriProvider(
    private val libraryConnections: ProvideLibraryConnections,
    private val serviceFileUriQueryParamsProvider: IServiceFileUriQueryParamsProvider
) : ProvideFileUrisForLibrary {
    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> {
        logger.debug("Returning URL from server.")

        /* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */
        return libraryConnections
			.promiseLibraryConnection(libraryId)
			.then { c -> c?.urlProvider?.getUrl(*serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(serviceFile))?.let(Uri::parse) }
    }
}
