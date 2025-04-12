package com.lasthopesoftware.resources.control.GivenManyForegroundNotifications

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class WhenStoppingOneForegroundNotification : AndroidContext() {

	companion object {
		private val service by lazy { spyk(Robolectric.buildService(PlaybackService::class.java).get()) }
	}

    override fun before() {
		val notificationsController = NotificationsController(service, mockk(relaxed = true, relaxUnitFun = true))
        notificationsController.notifyForeground(mockk(), 13)
        notificationsController.notifyForeground(mockk(), 33)
        notificationsController.notifyForeground(mockk(), 77)
        notificationsController.stopForegroundNotification(13)
    }

    @Test
    fun thenTheServiceStartsForegroundForEachForegroundNotification() {
		verify(exactly = 3) { service.startForeground(any(), any()) }
    }

    @Test
    fun thenTheServiceIsStillInTheForeground() {
		verify(exactly = 0) { service.stopForeground(any<Int>()) }
    }
}
