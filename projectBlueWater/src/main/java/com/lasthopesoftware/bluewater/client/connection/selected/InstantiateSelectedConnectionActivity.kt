package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.connection.session.ActivityConnectionInitializationController
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.keepPromise
import com.lasthopesoftware.bluewater.shared.promises.extensions.promiseActivityResult
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise

class InstantiateSelectedConnectionActivity : AppCompatActivity() {
	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val handler by lazy { Handler(mainLooper) }

	private val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	private val activityConnectionInitializationController by lazy {
		ActivityConnectionInitializationController(
			libraryConnectionProvider,
			ActivityApplicationNavigation(this),
		)
	}

	private val connectionStatusViewModel by buildViewModelLazily {
		ConnectionStatusViewModel(
			StringResources(this),
			activityConnectionInitializationController,
		)
	}

	private val selectedLibraryProvider by lazy { SelectedLibraryIdProvider(getApplicationSettingsRepository()) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				ConnectionUpdatesView(connectionStatusViewModel)
			}
		}

		selectedLibraryProvider
			.promiseSelectedLibraryId()
			.eventually { libraryId ->
				libraryId
					?.let(connectionStatusViewModel::ensureConnectionIsWorking)
					.keepPromise(false)
			}
			.eventually(LoopedInPromise.response({
				if (it) {
					if (intent?.action == START_ACTIVITY_FOR_RETURN) finishForResultDelayed()
					else launchActivityDelayed(browseLibraryIntent)
				} else {
					finish()
				}
			}, handler), LoopedInPromise.response({
				finish()
			}, handler))

		onBackPressedDispatcher.addCallback {
			with (connectionStatusViewModel) {
				if (isGettingConnection.value)
					cancelCurrentCheck()
				finish()
			}
		}
	}

	private fun launchActivityDelayed(intent: Intent) {
		if (isCancelled()) {
			finish()
			return
		}

		handler.postDelayed(
			{
				if (!isCancelled())
					startActivity(intent)
				finish()
			},
			ACTIVITY_LAUNCH_DELAY
		)
	}

	private fun finishForResultDelayed() {
		if (isCancelled()) {
			finish()
			return
		}

		handler.postDelayed(::finish, ACTIVITY_LAUNCH_DELAY)
	}

	private fun isCancelled() = connectionStatusViewModel.isCancelled

	companion object {
		private val START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName<InstantiateSelectedConnectionActivity>("START_ACTIVITY_FOR_RETURN")
		private const val ACTIVITY_LAUNCH_DELAY = 2500L

		fun restoreSelectedConnection(activity: ComponentActivity): Promise<ActivityResult?> =
			getInstance(activity).isSessionConnectionActive().eventually { isActive ->
				if (!isActive) activity.promiseActivityResult(
					Intent(activity, cls<InstantiateSelectedConnectionActivity>()).apply {
						action = START_ACTIVITY_FOR_RETURN
					}
				)
				else Promise.empty()
			}

		fun startNewConnection(context: Context) {
			context.startActivity(Intent(context, cls<InstantiateSelectedConnectionActivity>()))
		}
	}
}
