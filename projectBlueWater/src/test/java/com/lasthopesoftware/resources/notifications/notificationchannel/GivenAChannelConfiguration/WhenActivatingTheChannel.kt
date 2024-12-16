package com.lasthopesoftware.resources.notifications.notificationchannel.GivenAChannelConfiguration

import android.app.NotificationChannel
import com.lasthopesoftware.AndroidContext
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.ChannelConfiguration
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test

class WhenActivatingTheChannel : AndroidContext() {
	companion object {
		private var notificationChannel: NotificationChannel? = null

		private val channelActivator by lazy {
			NotificationChannelActivator(mockk {
				every { createNotificationChannel(any()) } answers {
					notificationChannel = firstArg()
					notificationChannel?.id
				}
			})
		}
	}

	override fun before() {
		channelActivator.activateChannel(object : ChannelConfiguration {
			override val channelId: String
				get() = "myActiveChannel"
			override val channelName: String
				get() = "a-name"
			override val channelDescription: String
				get() = "description"
			override val channelImportance: Int
				get() = 4
		})
	}

    @Test
    fun `then the channel name is correct`() {
        assertThat(notificationChannel?.name).isEqualTo("a-name")
    }

    @Test
    fun `then the channel id is correct`() {
        assertThat(notificationChannel?.id).isEqualTo("myActiveChannel")
    }

    @Test
    fun `then the channel description is correct`() {
        assertThat(notificationChannel?.description).isEqualTo("description")
    }

    @Test
    fun `then the channel importance is correct`() {
        assertThat(notificationChannel?.importance).isEqualTo(4)
    }
}
