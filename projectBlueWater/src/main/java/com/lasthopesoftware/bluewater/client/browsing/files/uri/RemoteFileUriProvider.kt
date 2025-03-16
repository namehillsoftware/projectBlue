package com.lasthopesoftware.bluewater.client.browsing.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.BuildConfig
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.uri.toUri
import com.namehillsoftware.handoff.promises.Promise

class RemoteFileUriProvider(private val libraryConnections: ProvideLibraryConnections) : ProvideFileUrisForLibrary {
	companion object {
		private val logger by lazyLogger<RemoteFileUriProvider>()
	}

    override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> {
		if (BuildConfig.DEBUG) {
			logger.debug("Returning URL from server.")
		}

        return libraryConnections
			.promiseLibraryConnection(libraryId)
			.then { c -> c?.getFileUrl(serviceFile)?.toURI()?.toUri() }
    }
}
