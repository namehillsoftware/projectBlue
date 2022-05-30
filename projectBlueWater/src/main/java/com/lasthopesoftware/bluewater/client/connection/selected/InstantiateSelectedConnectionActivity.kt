package com.lasthopesoftware.bluewater.client.connection.selected

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import com.lasthopesoftware.bluewater.R
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsActivity
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.view.LazyViewFinder
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.registerReceiver
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.promiseActivityResult
import com.namehillsoftware.handoff.promises.Promise

class InstantiateSelectedConnectionActivity : Activity(), (SelectedConnection.BuildSessionConnectionBroadcast) -> Unit {
	private var isCancelled = false
	private val lblConnectionStatus = LazyViewFinder<TextView>(this, R.id.lblConnectionStatus)
	private val cancelButton = LazyViewFinder<TextView>(this, R.id.cancelButton)

	private val selectServerIntent by lazy { Intent(this, ApplicationSettingsActivity::class.java) }

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val handler by lazy { Handler(mainLooper) }

	private val lazyPromisedSessionConnection = lazy { getInstance(this).promiseSessionConnection() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.layout_status)

		lblConnectionStatus.findView().setText(R.string.lbl_connecting)
		cancelButton.findView().setOnClickListener { cancel() }

		val applicationMessageBus = getApplicationMessageBus()
		applicationMessageBus.registerReceiver(this)

		lazyPromisedSessionConnection
			.value
			.eventually(LoopedInPromise.response({ c ->
				when {
					isCancelled -> Unit
					c == null -> launchActivityDelayed(selectServerIntent)
					intent?.action == START_ACTIVITY_FOR_RETURN -> finishForResultDelayed()
					else -> launchActivityDelayed(browseLibraryIntent)
				}
			}, handler), LoopedInPromise.response({
				launchActivityDelayed(selectServerIntent)
			}, handler))
			.must { applicationMessageBus.unregisterReceiver(this) }
	}

	override fun onBackPressed() {
		cancel()
		super.onBackPressed()
	}

	override fun invoke(message: SelectedConnection.BuildSessionConnectionBroadcast) {
		handleBuildStatusChange(message.buildingConnectionStatus)
	}

	private fun handleBuildStatusChange(status: BuildingConnectionStatus) {
		lblConnectionStatus.findView().setText(when (status) {
			BuildingConnectionStatus.GettingLibrary -> R.string.lbl_getting_library_details
			BuildingConnectionStatus.GettingLibraryFailed -> R.string.lbl_please_connect_to_valid_server
			BuildingConnectionStatus.SendingWakeSignal -> R.string.sending_wake_signal
			BuildingConnectionStatus.BuildingConnection -> R.string.lbl_connecting_to_server_library
			BuildingConnectionStatus.BuildingConnectionFailed -> R.string.lbl_error_connecting_try_again
			BuildingConnectionStatus.BuildingConnectionComplete -> R.string.lbl_connected
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
		private val START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName(
			InstantiateSelectedConnectionActivity::class.java, "START_ACTIVITY_FOR_RETURN")
		private const val ACTIVITY_LAUNCH_DELAY = 2500L

		fun restoreSelectedConnection(activity: ComponentActivity): Promise<ActivityResult?> =
			getInstance(activity).isSessionConnectionActive().eventually { isActive ->
				if (!isActive) activity.promiseActivityResult(
					Intent(activity, InstantiateSelectedConnectionActivity::class.java).apply {
						action = START_ACTIVITY_FOR_RETURN
					}
				)
				else Promise.empty()
			}

		fun startNewConnection(context: Context) {
			context.startActivity(Intent(context, InstantiateSelectedConnectionActivity::class.java))
		}
	}
}
