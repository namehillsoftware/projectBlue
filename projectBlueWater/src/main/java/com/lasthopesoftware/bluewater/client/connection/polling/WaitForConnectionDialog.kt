package com.lasthopesoftware.bluewater.client.connection.polling

import android.app.AlertDialog
import android.content.Context
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

object WaitForConnectionDialog {
    fun show(context: Context) {
        AlertHolder(context)
    }

    private class AlertHolder(context: Context) {
        private val alertDialog: AlertDialog
        private var isDismissed = false

        init {
            val message = context.getString(R.string.lbl_attempting_to_reconnect)
				.format(context.getString(R.string.app_name))
            val builder = AlertDialog.Builder(context, R.style.DialogTheme)
            builder
				.setTitle(context.getText(R.string.lbl_connection_lost_title))
				.setMessage(message)
				.setCancelable(true)
            val pollingSessionConnection = pollSessionConnection(context)
            builder.setNegativeButton(context.getText(R.string.btn_cancel)) { dialog, _ ->
                pollingSessionConnection.cancel()
                dialog.dismiss()
            }
            builder.setOnDismissListener { isDismissed = true }
            builder.setOnCancelListener { isDismissed = true }
            alertDialog = builder.create()
				.apply {
					setInverseBackgroundForced(true)
					setOnShowListener {
						pollingSessionConnection
							.must {
								LoopedInPromise({
									if (!isDismissed && isShowing) dismiss()
								}, context)
							}
					}
					show()
				}
        }
    }
}
