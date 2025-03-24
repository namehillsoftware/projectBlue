package com.lasthopesoftware.bluewater.permissions

import android.Manifest
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.client.browsing.library.settings.access.ProvideLibrarySettings
import com.lasthopesoftware.bluewater.permissions.read.ProvideReadPermissionsRequirements
import com.lasthopesoftware.bluewater.shared.android.permissions.CheckOsPermissions
import com.lasthopesoftware.bluewater.shared.android.permissions.ManagePermissions
import com.lasthopesoftware.promises.extensions.unitResponse
import com.namehillsoftware.handoff.promises.Promise

class ApplicationPermissionsRequests(
	private val librarySettingsProvider: ProvideLibrarySettings,
	private val applicationReadPermissionsRequirementsProvider: ProvideReadPermissionsRequirements,
	private val permissionsManager: ManagePermissions,
	private val checkOsPermissions: CheckOsPermissions,
) : RequestApplicationPermissions {
	override fun promiseApplicationPermissionsRequest(): Promise<Unit> =
		librarySettingsProvider
			.promiseAllLibrarySettings()
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

				if (checkOsPermissions.isForegroundMediaServicePermissionNotGranted) {
					permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK)
				}

				if (checkOsPermissions.isForegroundDataServicePermissionNotGranted) {
					permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
				}

				permissionsManager.requestPermissions(permissionsToRequest.toList())
			}
			.unitResponse()

	override fun promiseIsAllPermissionsGranted(library: LibrarySettings): Promise<Boolean> {
		val permissionsToRequest = ArrayList<String>(2)

		with (applicationReadPermissionsRequirementsProvider) {
			if (isReadMediaPermissionsRequiredForLibrary(library) || isReadMediaPermissionsRequiredForLibrary(library))
				permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
			if (isReadPermissionsRequiredForLibrary(library) || isReadPermissionsRequiredForLibrary(library))
				permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
		}

		return permissionsManager
			.requestPermissions(permissionsToRequest)
			.then { p -> p.values.all { it } }
	}
}
