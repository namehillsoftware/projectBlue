package com.lasthopesoftware.bluewater.client.connection.session

import android.content.Context
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class SessionConnectionProvider(private val context: Context) : ProvideSessionConnection {
	override fun promiseSessionConnection(): Promise<IConnectionProvider?> =
		SessionConnection.getInstance(context).promiseSessionConnection()
}
