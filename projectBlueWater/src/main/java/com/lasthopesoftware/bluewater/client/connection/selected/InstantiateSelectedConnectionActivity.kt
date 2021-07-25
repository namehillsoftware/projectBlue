package com.lasthopesoftware.bluewater.client.connection.selected

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.BuildingSessionConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise

class InstantiateSelectedConnectionActivity : Activity() {
	private val lblConnectionStatus = LazyViewFinder<TextView>(this, R.id.lblConnectionStatus)

	private val selectServerIntent = lazy { Intent(this, ApplicationSettingsActivity::class.java) }

	private val browseLibraryIntent = lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val localBroadcastManager = lazy { LocalBroadcastManager.getInstance(this) }

	private val handler = lazy { Handler(mainLooper) }

	private val buildSessionConnectionReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			handleBuildStatusChange(intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1))
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.layout_status)

		lblConnectionStatus.findView().setText(R.string.lbl_connecting)

		localBroadcastManager.value.registerReceiver(buildSessionConnectionReceiver, IntentFilter(
			SelectedConnection.buildSessionBroadcast
		))

		getInstance(this)
			.promiseSessionConnection()
			.eventually(LoopedInPromise.response({ c ->
				if (c == null)
					launchActivityDelayed(selectServerIntent.value)
				else if (intent == null || START_ACTIVITY_FOR_RETURN != intent.action)
					launchActivityDelayed(browseLibraryIntent.value)
				else
					finish()
			}, handler.value), LoopedInPromise.response({
				launchActivityDelayed(selectServerIntent.value)
			}, handler.value))
			.must { localBroadcastManager.value.unregisterReceiver(buildSessionConnectionReceiver) }
	}

	private fun handleBuildStatusChange(status: Int) {
		lblConnectionStatus.findView().setText(when (status) {
			BuildingSessionConnectionStatus.GettingLibrary -> R.string.lbl_getting_library_details
			BuildingSessionConnectionStatus.GettingLibraryFailed -> R.string.lbl_please_connect_to_valid_server
			BuildingSessionConnectionStatus.SendingWakeSignal -> R.string.sending_wake_signal
			BuildingSessionConnectionStatus.BuildingConnection -> R.string.lbl_connecting_to_server_library
			BuildingSessionConnectionStatus.BuildingConnectionFailed -> R.string.lbl_error_connecting_try_again
			BuildingSessionConnectionStatus.BuildingSessionComplete -> R.string.lbl_connected
			else -> R.string.lbl_connecting
		})
	}

	private fun launchActivityDelayed(intent: Intent) {
		handler.value.postDelayed({ startActivity(intent) }, ACTIVITY_LAUNCH_DELAY.toLong())
	}

	companion object {
		private const val ACTIVITY_ID = 2032
		private val START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName(
			InstantiateSelectedConnectionActivity::class.java, "START_ACTIVITY_FOR_RETURN")
		private const val ACTIVITY_LAUNCH_DELAY = 1500

		fun restoreSelectedConnection(activity: Activity): Int? {
			return when (getInstance(activity).isSessionConnectionActive()) {
				false -> {
					val intent = Intent(activity, InstantiateSelectedConnectionActivity::class.java)
					intent.action = START_ACTIVITY_FOR_RETURN
					activity.startActivityForResult(intent, ACTIVITY_ID)
					ACTIVITY_ID
				}
				else -> null
			}
		}

		fun startNewConnection(context: Context) {
			context.startActivity(Intent(context, InstantiateSelectedConnectionActivity::class.java))
		}
	}
}
