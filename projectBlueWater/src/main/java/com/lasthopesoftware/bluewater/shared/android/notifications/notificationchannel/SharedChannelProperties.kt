package com.lasthopesoftware.bluewater.shared.android.notifications.notificationchannel

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.lasthopesoftware.bluewater.R
import com.namehillsoftware.lazyj.AbstractSynchronousLazy
import com.namehillsoftware.lazyj.CreateAndHold

class SharedChannelProperties(private val context: Context) : ChannelConfiguration {
    private val lazyChannelName: CreateAndHold<String> =
        object : AbstractSynchronousLazy<String>() {
            override fun create(): String {
                return context.getString(R.string.app_name)
            }
        }
    private val lazyChannelDescription: CreateAndHold<String> =
        object : AbstractSynchronousLazy<String>() {
            override fun create(): String {
                return String.format("Notifications for %1\$s", lazyChannelName.getObject())
            }
        }
    override val channelId: String
        get() = Companion.channelId
    override val channelName: String
        get() = lazyChannelName.getObject()
    override val channelDescription: String
        get() = lazyChannelDescription.getObject()
    override val channelImportance: Int
        get() = Companion.channelImportance

    companion object {
        private const val channelId = "MusicCanoe"
        private val channelImportance =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) 3 else NotificationManager.IMPORTANCE_DEFAULT
    }
}
