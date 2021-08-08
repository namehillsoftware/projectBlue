package com.lasthopesoftware.bluewater.client.connection.authentication

import com.namehillsoftware.handoff.promises.Promise

interface CheckIfScopedConnectionIsAuthenticated {
	fun promiseIsAuthenticated(): Promise<Boolean>
}
