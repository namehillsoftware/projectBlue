package com.lasthopesoftware.bluewater.client.connection.authentication

import com.namehillsoftware.handoff.promises.Promise

interface CheckIfScopedConnectionIsAuthenticated {
	fun isAuthenticated(): Promise<Boolean>
}
