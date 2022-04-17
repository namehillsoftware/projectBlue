package com.lasthopesoftware.bluewater.client.browsing.items.media.files.uri

import android.net.Uri
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.IServiceFileUriQueryParamsProvider
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise
import org.slf4j.LoggerFactory

private val logger by lazy { LoggerFactory.getLogger(RemoteFileUriProvider::class.java) }

class RemoteFileUriProvider(
    private val connectionProvider: IConnectionProvider,
    private val serviceFileUriQueryParamsProvider: IServiceFileUriQueryParamsProvider
) : IFileUriProvider {
    override fun promiseFileUri(serviceFile: ServiceFile): Promise<Uri?> {
        logger.debug("Returning URL from server.")

        /* Playback:
		 * 0: Downloading (not real-time playback);
		 * 1: Real-time playback with update of playback statistics, Scrobbling, etc.;
		 * 2: Real-time playback, no playback statistics handling (default: )
		 */
        val itemUrl = connectionProvider.urlProvider
            .getUrl(*serviceFileUriQueryParamsProvider.getServiceFileUriQueryParams(serviceFile))
        return Promise(Uri.parse(itemUrl))
    }
}
