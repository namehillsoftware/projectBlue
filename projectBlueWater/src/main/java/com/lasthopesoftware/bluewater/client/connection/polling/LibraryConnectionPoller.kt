package com.lasthopesoftware.bluewater.client.connection.polling

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.promises.PromiseDelay
import com.namehillsoftware.handoff.Messenger
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy
import org.joda.time.Duration
import java.util.concurrent.CancellationException

interface ProvideConnectionPollTimes {
	fun getConnectionTimes(): Sequence<Duration>
}

object ConnectionPollTimes : ProvideConnectionPollTimes {
	private val initialDuration = Duration.standardSeconds(1)
	private val maxDuration = Duration.standardSeconds(32)

	override fun getConnectionTimes(): Sequence<Duration> = sequence {
		var connectionTime = initialDuration
		while (true) {
			yield(connectionTime)
			connectionTime = connectionTime.multipliedBy(2).takeIf { it.isShorterThan(maxDuration) } ?: maxDuration
		}
	}
}

class LibraryConnectionPoller(
	private val connectionSessions: ManageConnectionSessions,
	private val connectionPollTimes: ProvideConnectionPollTimes,
) : PollForLibraryConnections {
	override fun pollConnection(libraryId: LibraryId): Promise<LiveServerConnection> {
		return Promise<LiveServerConnection> {
			val cancellationProxy = CancellationProxy()
			it.awaitCancellation(cancellationProxy)
			pollLibraryConnection(libraryId, it, cancellationProxy, connectionPollTimes.getConnectionTimes().iterator())
		}
	}

	private fun pollLibraryConnection(libraryId: LibraryId, messenger: Messenger<LiveServerConnection>, cancellationProxy: CancellationProxy, connectionTimes: Iterator<Duration>) {
		if (cancellationProxy.isCancelled) {
			messenger.sendRejection(newCancellationException())
			return
		}

		PromiseDelay
			.delay<Any?>(connectionTimes.next())
			.also(cancellationProxy::doCancel)
			.then({
				if (cancellationProxy.isCancelled) {
					messenger.sendRejection(newCancellationException())
					return@then
				}

				connectionSessions
					.promiseTestedLibraryConnection(libraryId)
					.also(cancellationProxy::doCancel)
					.then({ cp ->
						if (cp == null) {
							pollLibraryConnection(libraryId, messenger, cancellationProxy, connectionTimes)
						} else {
							messenger.sendResolution(cp)
						}
					}, {
						pollLibraryConnection(libraryId, messenger, cancellationProxy, connectionTimes)
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
