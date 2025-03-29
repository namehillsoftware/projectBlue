package com.lasthopesoftware.bluewater.permissions.requests.GivenLibrariesWithAllRequiredPermissions

import android.Manifest
import com.lasthopesoftware.bluewater.client.browsing.library.settings.LibrarySettings
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When making a permissions request` {

	private val mut by lazy {
		ApplicationPermissionsRequests(
			mockk {
				every { promiseAllLibrarySettings() } returns listOf(LibrarySettings(), LibrarySettings()).toPromise()
			},
			mockk {
				every { isReadPermissionsRequiredForLibrary(any()) } returns true
				every { isReadMediaPermissionsRequiredForLibrary(any()) } returns true
			},
			mockk {
				every { requestPermissions(any()) } answers {
					requestedPermissions.addAll(firstArg())
					Promise(emptyMap())
				}
			},
			mockk {
				every { isNotificationsPermissionNotGranted } returns true
				every { isForegroundMediaServicePermissionNotGranted } returns false
				every { isForegroundDataServicePermissionNotGranted } returns true
			}
		)
	}

	private val requestedPermissions = ArrayList<String>()

	@BeforeAll
	fun act() {
		mut.promiseApplicationPermissionsRequest().toExpiringFuture().get()
	}

	@Test
	fun `then a permissions request is made correctly`() {
		assertThat(requestedPermissions).containsExactlyInAnyOrder(
			Manifest.permission.READ_MEDIA_AUDIO,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.POST_NOTIFICATIONS,
			Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
		)
	}
}
