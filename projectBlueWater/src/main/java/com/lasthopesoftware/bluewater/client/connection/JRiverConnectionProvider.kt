package com.lasthopesoftware.bluewater.client.connection

import com.lasthopesoftware.bluewater.client.access.JRiverLibraryAccess
import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.connection.okhttp.ProvideOkHttpClients
import com.lasthopesoftware.bluewater.client.connection.url.ProvideUrls
import com.lasthopesoftware.bluewater.client.connection.url.UrlKeyHolder
import com.lasthopesoftware.bluewater.shared.NonStandardResponseException
import com.lasthopesoftware.bluewater.shared.StandardResponse.Companion.toStandardResponse
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.IOException
import java.net.URL

class JRiverConnectionProvider(override val urlProvider: ProvideUrls, private val okHttpClients: ProvideOkHttpClients) : ProvideConnections {
	companion object {
		private val logger by lazyLogger<JRiverConnectionProvider>()
	}

	private val lazyOkHttpClient by lazy { okHttpClients.getOkHttpClient(urlProvider) }
	private val dataAccess by lazy { JRiverLibraryAccess(this) }

	override fun promiseResponse(vararg params: String): Promise<Response> =
		try {
			HttpPromisedResponse(callServer(*params))
		} catch (e: Throwable) {
			Promise(e)
		}

	override fun <T> getConnectionKey(key: T): UrlKeyHolder<T> = UrlKeyHolder(urlProvider.baseUrl, key)

	override fun getDataAccess(): RemoteLibraryAccess = dataAccess

	override fun promiseIsConnectionPossible(): Promise<Boolean> = ConnectionPossiblePromise(this)

	private fun callServer(vararg params: String): Call {
		val url = URL(urlProvider.getUrl(*params))
		val request = Request.Builder().url(url).build()
		return lazyOkHttpClient.newCall(request)
	}

	private class ConnectionPossiblePromise(connectionProvider: ProvideConnections) : Promise<Boolean>() {
		init {
			val cancellationProxy = CancellationProxy()
			awaitCancellation(cancellationProxy)

			connectionProvider
				.promiseResponse("Alive")
				.also(cancellationProxy::doCancel)
				.then(
					{ it, cp -> resolve(testResponse(it, cp)) },
					{ e, _ ->
						logger.error("Error checking connection at URL {}.", connectionProvider.urlProvider.baseUrl, e)
						resolve(false)
					}
				)
				.also(cancellationProxy::doCancel)
		}

		private fun testResponse(response: Response, cancellationSignal: CancellationSignal): Boolean {
			response.body.use { body ->
				if (cancellationSignal.isCancelled) return false

				try {
					return body.string().let { Jsoup.parse(it, Parser.xmlParser()) }.toStandardResponse().isStatusOk
				} catch (e: NonStandardResponseException) {
					logger.warn("Non standard response received.", e)
				} catch (e: IOException) {
					logger.error("Error closing connection, device failure?", e)
				} catch (e: IllegalArgumentException) {
					logger.warn("Illegal argument passed in", e)
				} catch (t: Throwable) {
					logger.error("Unexpected error parsing response.", t)
				}
			}
			return false
		}
	}
}
