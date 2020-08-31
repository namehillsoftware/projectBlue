package com.lasthopesoftware.bluewater.client.connection.polling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import java.util.concurrent.CancellationException

class WaitForConnectionActivity : Activity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_wait_for_connection)

		val selectServerIntent = Intent(this, ApplicationSettingsActivity::class.java)
		val pollSessionConnection = pollSessionConnection(this)

		findViewById<View>(R.id.btnCancel).setOnClickListener {
			pollSessionConnection.cancel()
			startActivity(selectServerIntent)
			finish()
		}

		pollSessionConnection
			.then(
				{ finish() },
				{ e ->
					if (e is CancellationException) startActivity(selectServerIntent)
					finish()
				})
	}

	companion object {
		fun beginWaiting(context: Context, onConnectionRegainedListener: Runnable) {
			context.startActivity(Intent(context, WaitForConnectionActivity::class.java))
			pollSessionConnection(context).then { onConnectionRegainedListener.run() }
		}
	}
}
