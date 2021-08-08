package com.lasthopesoftware.bluewater.client.connection.authentication

import com.lasthopesoftware.bluewater.client.connection.selected.ProvideSelectedConnection
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionAuthenticationChecker(private val selectedConnection: ProvideSelectedConnection) : CheckIfScopedConnectionIsAuthenticated {
	override fun isAuthenticated(): Promise<Boolean> =
		selectedConnection.promiseSessionConnection().then { it?.urlProvider?.authCode != null }
}
