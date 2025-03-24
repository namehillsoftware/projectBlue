package com.lasthopesoftware.bluewater.permissions

import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.namehillsoftware.handoff.promises.Promise

interface RequestApplicationPermissions {
	fun promiseApplicationPermissionsRequest(): Promise<Unit>

	fun promiseIsAllPermissionsGranted(library: LibrarySettings): Promise<Boolean>
}
