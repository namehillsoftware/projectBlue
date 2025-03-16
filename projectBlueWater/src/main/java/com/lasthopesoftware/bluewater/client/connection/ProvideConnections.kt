package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

interface ProvideConnections : RemoteLibraryAccess {
	val serverConnection: ServerConnection
	fun <T> getConnectionKey(key: T): UrlKeyHolder<T>
	fun getFileUrl(serviceFile: ServiceFile): URL

	fun promiseIsConnectionPossible(): Promise<Boolean>
}
