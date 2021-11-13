package com.lasthopesoftware.bluewater.client.stored.service.receivers.GivenSyncHasStarted

import android.content.Intent
import com.lasthopesoftware.bluewater.client.stored.sync.StoredFileSynchronization
import com.lasthopesoftware.bluewater.client.stored.sync.notifications.PostSyncNotification
import com.lasthopesoftware.bluewater.client.stored.sync.receivers.SyncStartedReceiver
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

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
            receiver.onReceive(
                mockk(),
                Intent(StoredFileSynchronization.onSyncStartEvent)
            )
        }
    }

	@Test
	fun thenNotificationsBegin() {
		Assertions.assertThat(notifications).containsExactly(null as String?)
	}
}
