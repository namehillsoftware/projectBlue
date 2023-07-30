package com.lasthopesoftware.bluewater.permissions.requests.GivenLibrariesWithMediaReadAndNotificationPermissionsRequired

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
		val mediaRequiredLibrary = Library()
		ApplicationPermissionsRequests(
			mockk {
				every { allLibraries } returns Promise(
					listOf(Library(), mediaRequiredLibrary)
				)
			},
			mockk {
				every { isReadPermissionsRequiredForLibrary(any()) } returns false
				every { isReadMediaPermissionsRequiredForLibrary(any()) } returns false
				every { isReadMediaPermissionsRequiredForLibrary(mediaRequiredLibrary) } returns true
			},
			mockk {
				every { isWritePermissionsRequiredForLibrary(any()) } returns false
			},
			mockk {
				every { requestPermissions(any()) } answers {
					requestedPermissions.addAll(firstArg())
					Promise(emptyMap())
				}
			},
			mockk {
				every { isNotificationsPermissionNotGranted } returns true
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
			Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS
		)
	}
}
