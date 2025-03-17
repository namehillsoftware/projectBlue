package com.lasthopesoftware.bluewater.client.connection.session.GivenALibrary.AndItsConnectionIsStillAlive

import android.app.Notification
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.LiveServerConnection
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotificationsConfiguration
import com.lasthopesoftware.bluewater.client.playback.nowplaying.broadcasters.notification.NotifyingLibraryConnectionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.DeferredProgressingPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.toExpiringFuture
import com.lasthopesoftware.resources.strings.FakeStringResources
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

private const val libraryId = 481

class `When initializing the connection with notifications` {

	private val foregroundNotifications = ArrayList<Notification>()

	private val mut by lazy {
		val deferredProgressingPromise = DeferredProgressingPromise<BuildingConnectionStatus, LiveServerConnection?>()

		Pair(
			deferredProgressingPromise,
            NotifyingLibraryConnectionProvider(
				mockk {
					every { getNotificationBuilder("zgAWWovHOi") } returns mockk(relaxed = true, relaxUnitFun = true)
				},
                mockk {
                    every { promiseLibraryConnection(LibraryId(libraryId)) } returns deferredProgressingPromise
                },
                NotificationsConfiguration("zgAWWovHOi", 194),
				mockk {
					every { notifyForeground(any(), any()) } answers {
						foregroundNotifications.add(firstArg())
					}
				},
				FakeStringResources(),
            )
		)
	}

	private var initializedConnection: LiveServerConnection? = null

	@BeforeAll
	fun act() {
		val (deferredPromise, controller) = mut

		deferredPromise.sendProgressUpdates(
			BuildingConnectionStatus.BuildingConnection,
		)
		deferredPromise.sendResolution(mockk())

		initializedConnection = controller.promiseLibraryConnection(LibraryId(libraryId)).toExpiringFuture().get()!!
	}

	@Test
	fun `then no notifications are sent`() {
		assertThat(foregroundNotifications).isEmpty()
	}

	@Test
    fun `then the connection is initialized`() {
		assertThat(initializedConnection).isNotNull
	}
}
