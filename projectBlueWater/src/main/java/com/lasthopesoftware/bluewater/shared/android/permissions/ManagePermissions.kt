package com.lasthopesoftware.bluewater.shared.android.permissions

import com.namehillsoftware.handoff.promises.Promise

interface ManagePermissions {
	fun requestPermissions(permissions: List<String>): Promise<Map<String, Boolean>>
}
