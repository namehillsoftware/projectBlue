package com.lasthopesoftware.bluewater.client.playback.caching.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.uri.PathAndQuery
import com.namehillsoftware.handoff.promises.Promise

class CachedAudioFileUriProvider(
	private val remoteFileUriProvider: RemoteFileUriProvider,
	private val cachedFilesProvider: CacheFiles
) : IFileUriProvider {
	override fun promiseFileUri(serviceFile: ServiceFile): Promise<Uri?> =
		remoteFileUriProvider.promiseFileUri(serviceFile)
			.eventually { uri ->
				uri
					?.let(PathAndQuery::forUri)
					?.let(cachedFilesProvider::promiseCachedFile)
					?.then { cachedFile -> cachedFile?.let(Uri::fromFile) }
					.keepPromise()
			}
}
