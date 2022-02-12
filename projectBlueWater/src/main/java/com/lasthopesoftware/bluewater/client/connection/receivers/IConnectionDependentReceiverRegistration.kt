package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents

interface IConnectionDependentReceiverRegistration {
    fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): ReceiveBroadcastEvents
    fun forIntents(): Collection<IntentFilter>
}
