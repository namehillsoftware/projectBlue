package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

interface RegisterReceiverForEvents {
    fun registerBroadcastEventsWithConnectionProvider(connectionProvider: IConnectionProvider): ReceiveBroadcastEvents
    fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): (ApplicationMessage) -> Unit
    fun forIntents(): Collection<IntentFilter>
    fun forClasses(): Collection<Class<ApplicationMessage>>
}
