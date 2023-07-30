package com.lasthopesoftware.bluewater.permissions

import com.namehillsoftware.handoff.promises.Promise

interface RequestApplicationPermissions {
	fun promiseApplicationPermissionsRequest(): Promise<Unit>
}
