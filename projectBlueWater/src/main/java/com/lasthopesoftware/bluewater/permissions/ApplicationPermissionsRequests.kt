package com.lasthopesoftware.bluewater.permissions

import android.Manifest
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.read.ProvideReadPermissionsRequirements
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class ApplicationPermissionsRequests(
	private val libraryProvider: ILibraryProvider,
	private val applicationReadPermissionsRequirementsProvider: ProvideReadPermissionsRequirements,
	private val permissionsManager: ManagePermissions,
	private val checkOsPermissions: CheckOsPermissions,
) : RequestApplicationPermissions {
	override fun promiseApplicationPermissionsRequest(): Promise<Unit> =
		libraryProvider
			.allLibraries
			.eventually { libraries ->
				val permissionsToRequest = HashSet<String>(4)
				for (library in libraries) {
					if (!permissionsToRequest.contains(Manifest.permission.READ_MEDIA_AUDIO) && applicationReadPermissionsRequirementsProvider.isReadMediaPermissionsRequiredForLibrary(library))
						permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
					if (!permissionsToRequest.contains(Manifest.permission.READ_EXTERNAL_STORAGE) && applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library))
						permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
				}

				if (checkOsPermissions.isNotificationsPermissionNotGranted) {
					permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
				}

				permissionsManager.requestPermissions(permissionsToRequest.toList())
			}
			.unitResponse()

	override fun promiseIsLibraryPermissionsGranted(library: Library): Promise<Boolean> {
		val permissionsToRequest = ArrayList<String>(3)
		if (applicationReadPermissionsRequirementsProvider.isReadMediaPermissionsRequiredForLibrary(library))
			permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
		if (applicationReadPermissionsRequirementsProvider.isReadPermissionsRequiredForLibrary(library))
			permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)

		return permissionsManager
			.requestPermissions(permissionsToRequest.toList())
			.then { p -> p.values.all { it } }
	}
}
