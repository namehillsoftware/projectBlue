package com.lasthopesoftware.resources.notifications.notificationchannel.GivenAChannelConfiguration

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.ChannelConfiguration
import com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel.NotificationChannelActivator
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RequiresApi(api = Build.VERSION_CODES.O)
@RunWith(RobolectricTestRunner::class)
class WhenActivatingTheChannel {
	companion object {
		private val notificationManager by lazy {
			ApplicationProvider.getApplicationContext<Context>().getSystemService(
				Context.NOTIFICATION_SERVICE
			) as NotificationManager
		}
		private val channelId by lazy {
			val activeNotificationChannelId = NotificationChannelActivator(notificationManager)
			activeNotificationChannelId.activateChannel(object : ChannelConfiguration {
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

		private val notificationChannel by lazy { notificationManager.getNotificationChannel(channelId) }
	}

    @Test
    fun `then the returned channel id is correct`() {
        assertThat(channelId).isEqualTo("myActiveChannel")
    }

    @Test
    fun `then the channel name is correct`() {
        assertThat(notificationChannel.name).isEqualTo("a-name")
    }

    @Test
    fun `then the channel id is correct`() {
        assertThat(notificationChannel.id).isEqualTo("myActiveChannel")
    }

    @Test
    fun `then the channel description is correct`() {
        assertThat(notificationChannel.description).isEqualTo("description")
    }

    @Test
    fun `then the channel importance is correct`() {
        assertThat(notificationChannel.importance).isEqualTo(4)
    }
}
