package com.lasthopesoftware.bluewater.client.connection.polling

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.promises.PromiseDelay
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import org.joda.time.Duration
import java.util.concurrent.CancellationException

class LibraryConnectionPoller(
	private val connectionSessions: ManageConnectionSessions
) : PollForLibraryConnections {
	override fun pollConnection(libraryId: LibraryId): Promise<IConnectionProvider> {
		return Promise<IConnectionProvider> {
			val cancellationProxy = CancellationProxy()
			it.promisedCancellation().must(cancellationProxy)
			pollLibraryConnection(libraryId, it, cancellationProxy, 1000L)
		}
	}

	private fun pollLibraryConnection(libraryId: LibraryId, messenger: Messenger<IConnectionProvider>, cancellationProxy: CancellationProxy, connectionTime: Long) {
		if (cancellationProxy.isCancelled) {
			messenger.sendRejection(newCancellationException())
			return
		}

		PromiseDelay
			.delay<Any?>(Duration.millis(connectionTime))
			.also(cancellationProxy::doCancel)
			.then({
				if (cancellationProxy.isCancelled) {
					messenger.sendRejection(newCancellationException())
					return@then
				}

				val nextConnectionTime = (connectionTime * 2).coerceAtMost(32_000)
				connectionSessions
					.promiseTestedLibraryConnection(libraryId)
					.also(cancellationProxy::doCancel)
					.then({ cp ->
						if (cp == null) {
							pollLibraryConnection(libraryId, messenger, cancellationProxy, nextConnectionTime)
						} else {
							messenger.sendResolution(cp)
						}
					}, {
						pollLibraryConnection(libraryId, messenger, cancellationProxy, nextConnectionTime)
					})
			}, { e ->
				if (cancellationProxy.isCancelled)
					messenger.sendRejection(newCancellationException())
				else
					messenger.sendRejection(e)
			})
	}

	private fun newCancellationException() = CancellationException("pollLibraryConnection was cancelled")
}
