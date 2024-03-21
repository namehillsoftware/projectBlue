package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.ProvideConnections
import com.namehillsoftware.handoff.promises.Promise

class ScopedConnectionAuthenticationChecker(private val connectionProvider: ProvideConnections) : CheckIfScopedConnectionIsReadOnly {
	override fun promiseIsReadOnly(): Promise<Boolean> = connectionProvider.promiseIsReadOnly()
}
