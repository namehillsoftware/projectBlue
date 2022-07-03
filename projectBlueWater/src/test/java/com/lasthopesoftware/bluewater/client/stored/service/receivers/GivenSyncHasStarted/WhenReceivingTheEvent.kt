package com.lasthopesoftware.bluewater.client.stored.service.receivers.GivenSyncHasStarted

import com.lasthopesoftware.bluewater.client.stored.sync.SyncStateMessage
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncStartedReceiver
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class WhenReceivingTheEvent {

	companion object {
		private val notifications: MutableCollection<String?> = ArrayList()

		@JvmStatic
		@BeforeClass
		fun context() {
			val syncNotification = mockk<PostSyncNotification>()
			with(syncNotification) {
				every { notify(any()) } answers {
					notifications.add(firstArg())
				}
			}

			val receiver = SyncStartedReceiver(syncNotification)
			receiver(SyncStateMessage.SyncStarted)
		}
	}

	@Test
	fun thenNotificationsBegin() {
		assertThat(notifications).containsExactly(null as String?)
	}
}
