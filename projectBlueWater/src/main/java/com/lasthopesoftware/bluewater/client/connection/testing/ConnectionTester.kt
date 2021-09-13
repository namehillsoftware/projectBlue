package com.lasthopesoftware.bluewater.client.connection.testing

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.StandardRequest
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.IOException

object ConnectionTester : TestConnections {
	private val logger = LoggerFactory.getLogger(ConnectionTester::class.java)

	override fun promiseIsConnectionPossible(connectionProvider: IConnectionProvider): Promise<Boolean> =
		ConnectionPossiblePromise(connectionProvider)

	private class ConnectionPossiblePromise(connectionProvider: IConnectionProvider) : Promise<Boolean>() {
		init {
			val cancellationProxy = CancellationProxy()
			respondToCancellation(cancellationProxy)

			connectionProvider.promiseResponse("Alive")
				.also(cancellationProxy::doCancel)
				.then({ resolve(!cancellationProxy.isCancelled && testResponse(it)) }, { resolve(false) })
		}
	}

	private fun testResponse(response: Response): Boolean {
		response.body?.use { body ->
			try {
				return body.byteStream().use(StandardRequest::fromInputStream)?.isStatus ?: false
			} catch (e: IOException) {
				logger.error("Error closing connection, device failure?", e)
			} catch (e: IllegalArgumentException) {
				logger.warn("Illegal argument passed in", e)
			}
		}
		return false
	}
}
