package com.lasthopesoftware.bluewater.client.playback.caching.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.files.uri.ProvideFileUrisForLibrary
import com.lasthopesoftware.bluewater.client.browsing.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.keepPromise
import com.lasthopesoftware.resources.uri.PathAndQuery.pathAndQuery
import com.namehillsoftware.handoff.promises.Promise

class CachedAudioFileUriProvider(
	private val remoteFileUriProvider: RemoteFileUriProvider,
	private val cachedFilesProvider: CacheFiles
) : ProvideFileUrisForLibrary {
	override fun promiseUri(libraryId: LibraryId, serviceFile: ServiceFile): Promise<Uri?> =
		remoteFileUriProvider
			.promiseUri(libraryId, serviceFile)
			.eventually { uri ->
				uri
					?.pathAndQuery()
					?.let { cachedFilesProvider.promiseCachedFile(libraryId, it) }
					?.then { cachedFile -> cachedFile?.let(Uri::fromFile) }
					.keepPromise()
			}
}
