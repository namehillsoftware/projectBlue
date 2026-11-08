package com.lasthopesoftware.bluewater.client.connection.live

import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

interface LiveServerConnection {
	val dataAccess: RemoteLibraryAccess
	fun promiseDataSourceFactory(): Promise<HttpDataSource.Factory>

	fun <T> getConnectionKey(key: T): UrlKeyHolder<T>
	fun getFileUrl(serviceFile: ServiceFile): URL
	fun promiseIsConnectionPossible(): Promise<Boolean>
}
