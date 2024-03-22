package com.lasthopesoftware.bluewater.client.connection.testing

import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.lasthopesoftware.bluewater.shared.StandardResponse
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException

object ConnectionTester : TestConnections {
	private val logger = LoggerFactory.getLogger(ConnectionTester::class.java)

	override fun promiseIsConnectionPossible(connectionProvider: ProvideConnections): Promise<Boolean> =
		ConnectionPossiblePromise(connectionProvider)

	private class ConnectionPossiblePromise(connectionProvider: ProvideConnections) : Promise<Boolean>() {
		init {
			val cancellationProxy = CancellationProxy()
			awaitCancellation(cancellationProxy)

			connectionProvider.promiseResponse("Alive")
				.also(cancellationProxy::doCancel)
				.then({ it, cp -> resolve(!cp.isCancelled && testResponse(it)) }, { _, _ -> resolve(false) })
				.also(cancellationProxy::doCancel)
		}
	}

	private fun testResponse(response: Response): Boolean {
		response.body.use { body ->
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
