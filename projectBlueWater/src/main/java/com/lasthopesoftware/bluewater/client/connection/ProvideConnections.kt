package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response

interface ProvideConnections {
	fun promiseResponse(vararg params: String): Promise<Response>
	val urlProvider: ProvideUrls
	fun <T> getConnectionKey(key: T): UrlKeyHolder<T>
	fun getDataAccess(): RemoteLibraryAccess

	fun promiseIsConnectionPossible(): Promise<Boolean>
}
