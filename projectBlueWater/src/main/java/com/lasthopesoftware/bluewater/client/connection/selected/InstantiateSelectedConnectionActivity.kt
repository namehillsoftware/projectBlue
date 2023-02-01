package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionInitializationController
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InstantiateSelectedConnectionActivity : AppCompatActivity() {
	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val handler by lazy { Handler(mainLooper) }

	private val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	private val connectionInitializationController by lazy {
		ConnectionInitializationController(
			libraryConnectionProvider,
			ActivityApplicationNavigation(this),
		)
	}

	private val connectionStatusViewModel by buildViewModelLazily {
		ConnectionStatusViewModel(
			StringResources(this),
			connectionInitializationController,
		).apply {
			onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(false) {
				init {
				    isGettingConnection.onEach {
						isEnabled = it
					}.launchIn(lifecycleScope)
				}

				override fun handleOnBackPressed() {
					cancelCurrentCheck()
				}
			})
		}
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
				}
			}, handler))
	}

	private fun launchActivityDelayed(intent: Intent) {
		if (!isCancelled())
			handler.postDelayed({ if (!isCancelled()) startActivity(intent) }, ACTIVITY_LAUNCH_DELAY)
	}

	private fun finishForResultDelayed() {
		if (!isCancelled())
			handler.postDelayed({ if (!isCancelled()) finish() }, ACTIVITY_LAUNCH_DELAY)
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
