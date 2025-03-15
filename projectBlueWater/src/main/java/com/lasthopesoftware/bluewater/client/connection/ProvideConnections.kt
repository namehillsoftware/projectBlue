package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.connection.requests.HttpResponse
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise
import java.net.URL

interface ProvideConnections {
	fun promiseResponse(path: String, vararg params: String): Promise<HttpResponse>
	val serverConnection: ServerConnection
	fun <T> getConnectionKey(key: T): UrlKeyHolder<T>
	fun getDataAccess(): RemoteLibraryAccess
	fun getFileUrl(serviceFile: ServiceFile): URL

	fun promiseIsConnectionPossible(): Promise<Boolean>
}
