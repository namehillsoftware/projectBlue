package com.lasthopesoftware.bluewater.permissions

import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.namehillsoftware.handoff.promises.Promise

interface RequestApplicationPermissions {
	fun promiseApplicationPermissionsRequest(): Promise<Unit>

	fun promiseIsLibraryPermissionsGranted(library: Library): Promise<Boolean>
}
