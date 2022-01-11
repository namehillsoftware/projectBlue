package com.lasthopesoftware.resources.control.GivenManyForegroundNotifications.AndOneIsChangedToNotifyEither

import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackService
import com.lasthopesoftware.bluewater.shared.android.notifications.control.NotificationsController
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric

class WhenStoppingAllForegroundNotifications : AndroidContext() {

	companion object {
		private val service by lazy { spyk(Robolectric.buildService(PlaybackService::class.java).get()) }
	}

    override fun before() {
		val notificationsController = NotificationsController(service, mockk(relaxed = true, relaxUnitFun = true))
        notificationsController.notifyForeground(mockk(), 13)
        notificationsController.notifyForeground(mockk(), 33)
        notificationsController.notifyForeground(mockk(), 77)
        notificationsController.notifyEither(mockk(), 33)
        notificationsController.notifyBackground(mockk(), 88)
        notificationsController.stopForegroundNotification(13)
        notificationsController.stopForegroundNotification(77)
    }

    @Test
    fun thenTheServiceStartsForegroundForEachForegroundNotification() {
		verify(exactly = 3) { service.startForeground(any(), any()) }
    }

    @Test
    fun `then the service stays in the foreground because the either notification remained foreground`() {
		verify(exactly = 0) { service.stopForeground(any<Boolean>()) }
    }
}
