package com.lasthopesoftware.bluewater.client.connection

import android.content.Context
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionActivity
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class HandleViewIoException(private val context: Context, private val onConnectionRegainedListener: Runnable) : ImmediateResponse<Throwable, Unit> {

	override fun respond(e: Throwable) {
		if (!ConnectionLostExceptionFilter.isConnectionLostException(e)) throw e

		WaitForConnectionActivity.beginWaiting(context, onConnectionRegainedListener)
	}
}
