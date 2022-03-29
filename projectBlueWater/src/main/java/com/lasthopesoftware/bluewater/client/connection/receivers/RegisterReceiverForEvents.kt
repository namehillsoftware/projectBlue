package com.lasthopesoftware.bluewater.client.connection.receivers

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessage

interface RegisterReceiverForEvents {
    fun registerWithConnectionProvider(connectionProvider: IConnectionProvider): (ApplicationMessage) -> Unit
    fun forClasses(): Collection<Class<ApplicationMessage>>
}
