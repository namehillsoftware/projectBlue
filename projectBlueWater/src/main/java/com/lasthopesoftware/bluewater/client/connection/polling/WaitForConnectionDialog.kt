package com.lasthopesoftware.bluewater.client.connection.polling

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

object WaitForConnectionDialog {
    fun show(context: Context, libraryId: LibraryId) {
        AlertHolder(context, libraryId)
    }

    private class AlertHolder(context: Context, libraryId: LibraryId) {
		private val alertDialog: AlertDialog
		private var isDismissed = false

        init {
            val message = context.getString(R.string.lbl_attempting_to_reconnect)
				.format(context.getString(R.string.app_name))
            val builder = MaterialAlertDialogBuilder(context, R.style.AppTheme_DialogTheme)
            builder
				.setTitle(context.getText(R.string.lbl_connection_lost_title))
				.setMessage(message)
				.setCancelable(true)
            val pollingSessionConnection = pollSessionConnection(context, libraryId)
            builder.setNegativeButton(context.getText(R.string.btn_cancel)) { dialog, _ ->
                pollingSessionConnection.cancel()
                dialog.dismiss()
            }
            builder.setOnDismissListener { isDismissed = true }
            builder.setOnCancelListener { isDismissed = true }
            alertDialog = builder.create()
				.apply {
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
