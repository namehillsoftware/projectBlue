package com.lasthopesoftware.bluewater.client.settings

import com.lasthopesoftware.bluewater.permissions.RequestApplicationPermissions

interface PermissionsDependencies {
	val applicationPermissions: RequestApplicationPermissions
}
