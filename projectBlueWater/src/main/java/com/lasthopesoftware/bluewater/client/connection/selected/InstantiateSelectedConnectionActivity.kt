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
import com.namehillsoftware.handoff.promises.Promise

class InstantiateSelectedConnectionActivity : Activity() {
	private val lblConnectionStatus = LazyViewFinder<TextView>(this, R.id.lblConnectionStatus)
	private val cancelButton = LazyViewFinder<TextView>(this, R.id.cancelButton)

	private val selectServerIntent by lazy { Intent(this, ApplicationSettingsActivity::class.java) }

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(this) }

	private val handler by lazy { Handler(mainLooper) }

	private val buildSessionConnectionReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			handleBuildStatusChange(intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1))
		}
	}

	private val lazyPromisedSessionConnection = lazy { getInstance(this).promiseSessionConnection() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.layout_status)

		lblConnectionStatus.findView().setText(R.string.lbl_connecting)
		cancelButton.findView().setOnClickListener {
			lazyPromisedSessionConnection.value.cancel()
		}

		localBroadcastManager.registerReceiver(buildSessionConnectionReceiver, IntentFilter(
			SelectedConnection.buildSessionBroadcast
		))

		lazyPromisedSessionConnection
			.value
			.eventually(LoopedInPromise.response({ c ->
				if (c == null)
					launchActivityDelayed(selectServerIntent)
				else if (intent == null || START_ACTIVITY_FOR_RETURN != intent.action)
					launchActivityDelayed(browseLibraryIntent)
				else
					finish()
			}, handler), LoopedInPromise.response({
				launchActivityDelayed(selectServerIntent)
			}, handler))
			.must { localBroadcastManager.unregisterReceiver(buildSessionConnectionReceiver) }
	}

	override fun onBackPressed() {
		if (lazyPromisedSessionConnection.isInitialized())
			lazyPromisedSessionConnection.value.cancel()
		super.onBackPressed()
	}

	override fun onDestroy() {
		if (lazyPromisedSessionConnection.isInitialized())
			lazyPromisedSessionConnection.value.cancel()
		super.onDestroy()
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
		handler.postDelayed({ startActivity(intent) }, ACTIVITY_LAUNCH_DELAY.toLong())
	}

	companion object {
		private const val ACTIVITY_ID = 2032
		private val START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName(
			InstantiateSelectedConnectionActivity::class.java, "START_ACTIVITY_FOR_RETURN")
		private const val ACTIVITY_LAUNCH_DELAY = 1500

		fun restoreSelectedConnection(activity: Activity): Promise<Int?> =
			getInstance(activity).isSessionConnectionActive().then { isActive ->
				if (!isActive) {
					val intent = Intent(activity, InstantiateSelectedConnectionActivity::class.java)
					intent.action = START_ACTIVITY_FOR_RETURN
					activity.startActivityForResult(intent, ACTIVITY_ID)
					ACTIVITY_ID
				}
				else null
			}

		fun startNewConnection(context: Context) {
			context.startActivity(Intent(context, InstantiateSelectedConnectionActivity::class.java))
		}
	}
}
