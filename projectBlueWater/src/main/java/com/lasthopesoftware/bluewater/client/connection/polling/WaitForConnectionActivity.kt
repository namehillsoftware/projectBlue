package com.lasthopesoftware.bluewater.client.connection.polling

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.polling.PollConnectionService.Companion.pollSessionConnection
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
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

		val libraryId = intent.safelyGetParcelableExtra<LibraryId>(libraryIdProperty) ?: return

		val pollSessionConnection = pollSessionConnection(this, libraryId)

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
		private val libraryIdProperty by lazy { MagicPropertyBuilder.buildMagicPropertyName<WaitForConnectionActivity>("libraryId") }

		fun beginWaiting(context: Context, libraryId: LibraryId, onConnectionRegainedListener: Runnable) {
			val intent = context.getIntent<WaitForConnectionActivity>().apply {
				putExtra(libraryIdProperty, libraryId)
			}

			context.startActivity(intent)
			pollSessionConnection(context, libraryId).then { onConnectionRegainedListener.run() }
		}
	}
}
