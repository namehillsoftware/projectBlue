package com.lasthopesoftware.bluewater.client.playback.caching.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.access.ICachedFilesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.uri.PathAndQuery
import com.namehillsoftware.handoff.promises.Promise
import java.io.File

class CachedAudioFileUriProvider(
	private val remoteFileUriProvider: RemoteFileUriProvider,
	private val cachedFilesProvider: ICachedFilesProvider
) : IFileUriProvider {
	override fun promiseFileUri(serviceFile: ServiceFile): Promise<Uri?> =
		remoteFileUriProvider.promiseFileUri(serviceFile)
			.eventually { uri ->
				uri
					?.let(PathAndQuery::forUri)
					?.let(cachedFilesProvider::promiseCachedFile)
					?.then { cachedFile ->
						cachedFile
							?.run { File(fileName) }
							?.takeIf { it.exists() }
							?.let(Uri::fromFile)
					}
					.keepPromise()
			}
}
