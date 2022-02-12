package com.lasthopesoftware.bluewater.client.connection.receivers

import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider

interface IConnectionDependentReceiverRegistration {
    fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): BroadcastReceiver
    fun forIntents(): Collection<IntentFilter>
}
