package com.lasthopesoftware.bluewater.client.connection.libraries

import com.lasthopesoftware.bluewater.ApplicationDependencies
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.PromisedConnectionsRepository
import com.lasthopesoftware.bluewater.client.connection.testing.ConnectionTester
import com.lasthopesoftware.policies.retries.RecursivePromiseRetryHandler
import com.lasthopesoftware.policies.retries.RetryExecutionPolicy

class RetryingConnectionApplicationDependencies(private val inner: ApplicationDependencies) : ApplicationDependencies by inner {
	companion object {
		private val connectionsRepository = PromisedConnectionsRepository()
	}

	private val connectionLostRetryPolicy by lazy {
		RetryExecutionPolicy(ConnectionLostRetryHandler(RecursivePromiseRetryHandler))
	}

	override val connectionSessions by lazy {
		ConnectionSessionManager(
			ConnectionTester,
			DelegatingLibraryConnectionProvider(inner.libraryConnectionProvider, connectionLostRetryPolicy),
			connectionsRepository,
			inner.sendApplicationMessages,
		)
	}

	override val libraryConnectionProvider
		get() = connectionSessions
}
