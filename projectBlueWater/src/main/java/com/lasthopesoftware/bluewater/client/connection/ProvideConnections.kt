package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response

interface ProvideConnections {
	fun promiseResponse(vararg params: String): Promise<Response>
	val urlProvider: IUrlProvider
}
