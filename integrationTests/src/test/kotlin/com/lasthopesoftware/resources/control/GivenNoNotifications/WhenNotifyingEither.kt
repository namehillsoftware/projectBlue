package com.lasthopesoftware.resources.control.GivenNoNotifications

import android.app.NotificationManager
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class WhenNotifyingEither : AndroidContext() {
	companion object {
		private val notificationManager by lazy { mockk<NotificationManager>(relaxed = true, relaxUnitFun = true) }
		private val service by lazy { spyk(Robolectric.buildService(PlaybackService::class.java).get()) }
	}

	override fun before() {
		val notificationsController = NotificationsController(service, notificationManager)
		notificationsController.notifyEither(mockk(),448)
	}

	@Test
	fun `then the service never starts in the foreground`() {
		verify(exactly = 0) { service.startForeground(any(), any()) }
	}

	@Test
	fun `then the notifications are started`() {
		verify(exactly = 1) { notificationManager.notify(448, any()) }
	}
}
