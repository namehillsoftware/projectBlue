package com.lasthopesoftware.bluewater.client.connection.polling

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import java.util.concurrent.CancellationException

class WaitForConnectionActivity : AppCompatActivity() {
	private val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			this,
			IntentBuilder(this),
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_wait_for_connection)

		val pollSessionConnection = pollSessionConnection(this)

		findViewById<View>(R.id.btnCancel).setOnClickListener {
			pollSessionConnection.cancel()
			applicationNavigation.viewApplicationSettings()
			finish()
		}

		pollSessionConnection
			.then(
				{ finish() },
				{ e ->
					if (e is CancellationException) applicationNavigation.viewApplicationSettings()
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
