package com.lasthopesoftware.bluewater.client.connection.selected

import android.app.Activity
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
import com.lasthopesoftware.bluewater.shared.android.messages.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.android.messages.ReceiveBroadcastEvents
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.namehillsoftware.handoff.promises.Promise

class InstantiateSelectedConnectionActivity : Activity() {
	private var isCancelled = false
	private val lblConnectionStatus = LazyViewFinder<TextView>(this, R.id.lblConnectionStatus)
	private val cancelButton = LazyViewFinder<TextView>(this, R.id.cancelButton)

	private val selectServerIntent by lazy { Intent(this, ApplicationSettingsActivity::class.java) }

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val messageBus by lazy { ApplicationMessageBus(LocalBroadcastManager.getInstance(this)) }

	private val handler by lazy { Handler(mainLooper) }

	private val buildSessionConnectionReceiver =
		ReceiveBroadcastEvents { intent -> handleBuildStatusChange(intent.getIntExtra(SelectedConnection.buildSessionBroadcastStatus, -1)) }

	private val lazyPromisedSessionConnection = lazy { getInstance(this).promiseSessionConnection() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.layout_status)

		lblConnectionStatus.findView().setText(R.string.lbl_connecting)
		cancelButton.findView().setOnClickListener { cancel() }

		messageBus.registerReceiver(buildSessionConnectionReceiver, IntentFilter(
			SelectedConnection.buildSessionBroadcast
		))

		lazyPromisedSessionConnection
			.value
			.eventually(LoopedInPromise.response({ c ->
				if (isCancelled) return@response
				else if (c == null)
					launchActivityDelayed(selectServerIntent)
				else if (intent == null || START_ACTIVITY_FOR_RETURN != intent.action)
					launchActivityDelayed(browseLibraryIntent)
				else
					finishForResultDelayed()
			}, handler), LoopedInPromise.response({
				launchActivityDelayed(selectServerIntent)
			}, handler))
			.must { messageBus.unregisterReceiver(buildSessionConnectionReceiver) }
	}

	override fun onBackPressed() {
		cancel()
		super.onBackPressed()
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
		if (!isCancelled)
			handler.postDelayed({ if (!isCancelled) startActivity(intent) }, ACTIVITY_LAUNCH_DELAY)
	}

	private fun finishForResultDelayed() {
		if (!isCancelled)
			handler.postDelayed({ if (!isCancelled) finish() }, ACTIVITY_LAUNCH_DELAY)
	}

	private fun cancel() {
		isCancelled = true
		if (lazyPromisedSessionConnection.isInitialized())
			lazyPromisedSessionConnection.value.cancel()
		startActivity(selectServerIntent)
	}

	companion object {
		private const val ACTIVITY_ID = 2032
		private val START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName(
			InstantiateSelectedConnectionActivity::class.java, "START_ACTIVITY_FOR_RETURN")
		private const val ACTIVITY_LAUNCH_DELAY = 2500L

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
