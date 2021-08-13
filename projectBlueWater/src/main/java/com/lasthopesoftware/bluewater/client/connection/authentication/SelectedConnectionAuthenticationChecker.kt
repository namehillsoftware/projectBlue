package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionAuthenticationChecker(private val selectedConnection: ProvideSelectedConnection, private val innerFactory: (IConnectionProvider) -> CheckIfScopedConnectionIsReadOnly) : CheckIfScopedConnectionIsReadOnly {
	override fun promiseIsReadOnly(): Promise<Boolean> =
		selectedConnection.promiseSessionConnection().eventually { c -> c?.let(innerFactory)?.promiseIsReadOnly() ?: false.toPromise() }
}
