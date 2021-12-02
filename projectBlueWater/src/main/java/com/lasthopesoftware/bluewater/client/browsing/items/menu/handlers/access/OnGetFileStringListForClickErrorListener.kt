package com.lasthopesoftware.bluewater.client.browsing.items.menu.handlers.access

import android.view.View
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.client.connection.polling.WaitForConnectionDialog
import com.namehillsoftware.handoff.promises.response.ImmediateResponse

class OnGetFileStringListForClickErrorListener(
	private val view: View,
	private val onClickListener: View.OnClickListener
) : ImmediateResponse<Throwable?, Unit> {
    override fun respond(innerException: Throwable?) {
        if (!ConnectionLostExceptionFilter.isConnectionLostException(innerException))
        	throw innerException ?: return

		WaitForConnectionDialog.show(view.context)
		pollSessionConnection(view.context)
			.then { onClickListener.onClick(view) }
    }
}
