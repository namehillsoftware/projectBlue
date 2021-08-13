package com.lasthopesoftware.bluewater.client.connection.authentication

import com.namehillsoftware.handoff.promises.Promise

interface CheckIfScopedConnectionIsReadOnly {
	fun promiseIsReadOnly(): Promise<Boolean>
}
