package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.BrowserEntryActivity
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
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
import com.lasthopesoftware.bluewater.shared.promises.extensions.unitResponse
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise

class InstantiateSelectedConnectionActivity : AppCompatActivity() {
	private var isCancelled = false

	private val browseLibraryIntent by lazy {
		val browseLibraryIntent = Intent(this, BrowserEntryActivity::class.java)
		browseLibraryIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		browseLibraryIntent
	}

	private val handler by lazy { Handler(mainLooper) }

	private val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	private val connectionStatusViewModel = buildViewModelLazily {
		ConnectionStatusViewModel(
			StringResources(this),
			libraryConnectionProvider,
			ActivityApplicationNavigation(this),
		)
	}

	private val selectedLibraryProvider by lazy { SelectedLibraryIdProvider(getApplicationSettingsRepository()) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContent {
			ProjectBlueTheme {
				ConnectionUpdatesView(connectionStatusViewModel.value)
			}
		}

		selectedLibraryProvider
			.promiseSelectedLibraryId()
			.eventually { libraryId ->
				libraryId
					?.let { l ->
						connectionStatusViewModel
							.value
							.ensureConnectionIsWorking(l)
					}
					.keepPromise()
					.unitResponse()
			}
			.eventually(LoopedInPromise.response({ c ->
				if (c != null && intent?.action == START_ACTIVITY_FOR_RETURN) {
					if (intent?.action == START_ACTIVITY_FOR_RETURN) finishForResultDelayed()
				}
			}, handler))
	}

	@Deprecated("Deprecated in Java")
	override fun onBackPressed() {
		cancel()
		super.onBackPressed()
	}

	private fun finishForResultDelayed() {
		if (!isCancelled())
			handler.postDelayed({ if (!isCancelled) finish() }, ACTIVITY_LAUNCH_DELAY)
	}

	private fun cancel() {
		if (connectionStatusViewModel.isInitialized())
			connectionStatusViewModel.value.cancelCurrentCheck()
	}

	private fun isCancelled() = connectionStatusViewModel.isInitialized() && connectionStatusViewModel.value.isCancelled

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
