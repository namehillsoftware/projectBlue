package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.url.IUrlProvider
import com.lasthopesoftware.resources.executors.HttpThreadPoolExecutor
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.*

class ConnectionProvider(urlProvider: IUrlProvider, okHttpClients: ProvideOkHttpClients) : IConnectionProvider {
	private val okHttpClients: ProvideOkHttpClients
	private val lazyOkHttpClient: CreateAndHold<OkHttpClient> = object : AbstractSynchronousLazy<OkHttpClient>() {
		override fun create(): OkHttpClient {
			return okHttpClients.getOkHttpClient(urlProvider)
		}
	}

	override val urlProvider: IUrlProvider

	override fun promiseResponse(vararg params: String): Promise<Response> {
		return try {
			HttpPromisedResponse(callServer(*params))
		} catch (e: Throwable) {
			Promise(e)
		}
	}

	@Throws(MalformedURLException::class)
	private fun callServer(vararg params: String): Call {
		val url = URL(urlProvider.getUrl(*params))
		val request = Request.Builder().url(url).build()
		return lazyOkHttpClient.getObject().newCall(request)
	}

	override fun promiseSentPacket(packets: ByteArray): Promise<Unit> {
		return QueuedPromise(MessageWriter {
			val url = URL(urlProvider.baseUrl)
			val address = InetAddress.getByName(url.host)
			val packet = DatagramPacket(packets, packets.size, address, url.port)
			val socket = DatagramSocket()
			socket.use { it.send(packet) }
		}, HttpThreadPoolExecutor.executor)
	}

	init {
		requireNotNull(urlProvider) { "urlProvider != null" }
		this.urlProvider = urlProvider
		requireNotNull(okHttpClients) { "okHttpClients != null" }
		this.okHttpClients = okHttpClients
	}
}
