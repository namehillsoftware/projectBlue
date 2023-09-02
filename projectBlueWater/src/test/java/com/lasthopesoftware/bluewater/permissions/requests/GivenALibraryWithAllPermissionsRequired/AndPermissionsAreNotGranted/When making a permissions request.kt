package com.lasthopesoftware.bluewater.permissions.requests.GivenALibraryWithAllPermissionsRequired.AndPermissionsAreNotGranted

import android.Manifest
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library
import com.lasthopesoftware.bluewater.permissions.ApplicationPermissionsRequests
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.namehillsoftware.handoff.promises.Promise
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class `When making a permissions request` {

	private val mut by lazy {
		ApplicationPermissionsRequests(
			mockk(),
			mockk {
				every { isReadPermissionsRequiredForLibrary(any()) } returns true
				every { isReadMediaPermissionsRequiredForLibrary(any()) } returns true
			},
			mockk {
				every { requestPermissions(any()) } answers {
					val permissions = firstArg<List<String>>()
					requestedPermissions.addAll(permissions)
					var hasPermissions = false
					Promise(permissions.associateWith {
						hasPermissions = !hasPermissions
						hasPermissions
					})
				}
			},
			mockk {
				every { isNotificationsPermissionNotGranted } returns true
			}
		)
	}

	private val requestedPermissions = ArrayList<String>()
	private var isPermissionsGranted = false

	@BeforeAll
	fun act() {
		isPermissionsGranted = mut.promiseIsLibraryPermissionsGranted(Library()).toExpiringFuture().get() ?: false
	}

	@Test
	fun `then a permissions request is made correctly`() {
		assertThat(requestedPermissions).containsExactlyInAnyOrder(
			Manifest.permission.READ_MEDIA_AUDIO,
			Manifest.permission.READ_EXTERNAL_STORAGE,
		)
	}

	@Test
	fun `then the permissions are returned as not granted`() {
		assertThat(isPermissionsGranted).isFalse
	}
}
