package com.lasthopesoftware.resources.control.GivenManyForegroundNotifications.AndTheNotificationControllerIsClosed

import android.app.NotificationManager
import android.app.Service
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class `When Adding New Notifications` : AndroidContext() {

	companion object {
		private val notificationManager by lazy { mockk<NotificationManager>(relaxed = true, relaxUnitFun = true) }
		private val service by lazy { spyk(Robolectric.buildService(PlaybackService::class.java).get()) }
	}

    override fun before() {
		val notificationsController = NotificationsController(service, notificationManager).use {
			it.notifyBackground(mockk(), 932)
			it.notifyBackground(mockk(), 711)
			it
		}

		notificationsController.notifyBackground(mockk(), 285)
		notificationsController.notifyForeground(mockk(), 758)
		notificationsController.notifyEither(mockk(), 758)
    }

	@Test
	fun `then background notifications are started correctly`() {
		verify(exactly = 2) { notificationManager.notify(any(), any()) }
	}

    @Test
    fun `then the foreground notifications are started correctly`() {
		verify(exactly = 0) { service.startForeground(any(), any()) }
    }

    @Test
    fun `then the service is not in the foreground and the notification is removed`() {
		verify(atLeast = 1) { service.stopForeground(Service.STOP_FOREGROUND_REMOVE) }
    }
}
