package com.lasthopesoftware.resources.control.GivenOneForegroundNotification.AndItIsNotifiedEither

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class WhenStoppingTheForegroundNotification : AndroidContext() {
	companion object {
		private val service by lazy { spyk(Robolectric.buildService(PlaybackService::class.java).get()) }
	}

	override fun before() {
		val notificationsController = NotificationsController(service, mockk(relaxed = true, relaxUnitFun = true))
		notificationsController.notifyForeground(mockk(), 624)
		notificationsController.notifyEither(mockk(),624)
		notificationsController.stopForegroundNotification(624)
	}

	@Test
	fun thenTheServiceStartsForegroundForEachForegroundNotification() {
		verify(exactly = 1) { service.startForeground(any(), any()) }
	}

	@Test
	fun thenTheServiceGoesToBackgroundOnce() {
		verify(exactly = 1) { service.stopForeground(any<Boolean>()) }
	}
}
