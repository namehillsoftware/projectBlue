package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Context
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.namehillsoftware.handoff.promises.Promise

class SelectedConnectionProvider(private val context: Context) : ProvideSelectedConnection {
	override fun promiseSessionConnection(): Promise<IConnectionProvider?> =
		SelectedConnection.getInstance(context).promiseSessionConnection()
}
