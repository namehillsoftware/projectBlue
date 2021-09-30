package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.namehillsoftware.handoff.promises.Promise
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import java.net.URL

class ConnectionProvider(override val urlProvider: IUrlProvider, private val okHttpClients: ProvideOkHttpClients) : IConnectionProvider {
	private val lazyOkHttpClient by lazy { okHttpClients.getOkHttpClient(urlProvider) }

	override fun promiseResponse(vararg params: String): Promise<Response> =
		try {
			HttpPromisedResponse(callServer(*params))
		} catch (e: Throwable) {
			Promise(e)
		}

	private fun callServer(vararg params: String): Call {
		val url = URL(urlProvider.getUrl(*params))
		val request = Request.Builder().url(url).build()
		return lazyOkHttpClient.newCall(request)
	}
}
