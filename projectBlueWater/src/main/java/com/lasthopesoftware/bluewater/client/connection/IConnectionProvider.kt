package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Response

interface IConnectionProvider {
	fun promiseResponse(vararg params: String): Promise<Response>
	fun promiseSentPacket(packet: ByteArray): Promise<Unit>
	val urlProvider: IUrlProvider
}
