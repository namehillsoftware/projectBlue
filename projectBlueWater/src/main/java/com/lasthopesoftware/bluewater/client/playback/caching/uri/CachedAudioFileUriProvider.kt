package com.lasthopesoftware.bluewater.client.playback.caching.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.CacheFiles
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.IFileUriProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri.RemoteFileUriProvider
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.resources.uri.PathAndQuery.pathAndQuery
import com.namehillsoftware.handoff.promises.Promise

class CachedAudioFileUriProvider(
	private val applicationSettings: HoldApplicationSettings,
	private val remoteFileUriProvider: RemoteFileUriProvider,
	private val cachedFilesProvider: CacheFiles
) : IFileUriProvider {
	override fun promiseFileUri(serviceFile: ServiceFile): Promise<Uri?> =
		applicationSettings.promiseApplicationSettings()
			.eventually { settings ->
				if (!settings.isUsingCustomCaching) Promise.empty()
				else remoteFileUriProvider.promiseFileUri(serviceFile)
					.eventually { uri ->
						uri
							?.pathAndQuery()
							?.let(cachedFilesProvider::promiseCachedFile)
							?.then { cachedFile -> cachedFile?.let(Uri::fromFile) }
							.keepPromise()
					}
			}
}
