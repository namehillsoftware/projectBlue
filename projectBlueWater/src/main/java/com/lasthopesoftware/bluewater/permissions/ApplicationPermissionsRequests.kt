package com.lasthopesoftware.bluewater.permissions

import android.Manifest
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.permissions.read.ProvideReadPermissionsRequirements
import com.lasthopesoftware.bluewater.permissions.write.ProvideWritePermissionsRequirements
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class ApplicationPermissionsRequests(
	private val libraryProvider: ILibraryProvider,
	private val applicationReadPermissionsRequirementsProvider: ProvideReadPermissionsRequirements,
	private val applicationWritePermissionsRequirementsProvider: ProvideWritePermissionsRequirements,
	private val permissionsManager: ManagePermissions,
	private val checkOsPermissions: CheckOsPermissions,
) : RequestApplicationPermissions {
	override fun promiseApplicationPermissionsRequest(): Promise<Unit> {
		return libraryProvider
			.allLibraries
			.eventually { libraries ->
				var requestedPermissions = libraries
					.flatMap { l ->
						val permissionsToRequest = ArrayList<String>(3)
						if (applicationReadPermissionsRequirementsProvider.isReadMediaPermissionsRequiredForLibrary(l))
							permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
						if (applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(l))
							permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
						if (applicationWritePermissionsRequirementsProvider.isWritePermissionsRequiredForLibrary(l))
							permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

						permissionsToRequest
					}
					.distinct()

				if (!checkOsPermissions.isNotificationsPermissionGranted) {
					requestedPermissions += Manifest.permission.POST_NOTIFICATIONS
				}

				permissionsManager.requestPermissions(requestedPermissions)
			}
			.unitResponse()
	}
}
