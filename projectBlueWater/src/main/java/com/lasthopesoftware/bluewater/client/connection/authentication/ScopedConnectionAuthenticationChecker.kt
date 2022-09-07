package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class ScopedConnectionAuthenticationChecker(private val connectionProvider: IConnectionProvider) : CheckIfScopedConnectionIsReadOnly {
	override fun promiseIsReadOnly(): Promise<Boolean> = connectionProvider.promiseIsReadOnly()
}
