package com.lasthopesoftware.bluewater.client.connection.testing

import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.namehillsoftware.handoff.cancellation.CancellationSignal
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import okhttp3.Response
import java.io.IOException

object ConnectionTester : TestConnections {
	private val logger by lazyLogger<ConnectionTester>()

	override fun promiseIsConnectionPossible(connectionProvider: ProvideConnections): Promise<Boolean> =
		ConnectionPossiblePromise(connectionProvider)

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
	}

	private fun testResponse(response: Response, cancellationSignal: CancellationSignal): Boolean {
		response.body.use { body ->
			if (cancellationSignal.isCancelled) return false

			try {
				return body.byteStream().use(StandardResponse::fromInputStream)?.isStatus ?: false
			} catch (e: IOException) {
				logger.error("Error closing connection, device failure?", e)
			} catch (e: IllegalArgumentException) {
				logger.warn("Illegal argument passed in", e)
			}
		}
		return false
	}
}
