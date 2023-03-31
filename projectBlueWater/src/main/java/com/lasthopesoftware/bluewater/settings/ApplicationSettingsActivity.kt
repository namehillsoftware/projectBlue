package com.lasthopesoftware.bluewater.settings

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.promises.extensions.LoopedInPromise
import com.lasthopesoftware.resources.closables.lazyScoped
import com.namehillsoftware.handoff.promises.queued.MessageWriter

private val logger by lazyLogger<ApplicationSettingsActivity>()

class ApplicationSettingsActivity : AppCompatActivity() {
	private val applicationSettingsRepository by lazy { getApplicationSettingsRepository() }
	private val selectedPlaybackEngineTypeAccess by lazy {
		SelectedPlaybackEngineTypeAccess(
			applicationSettingsRepository,
			DefaultPlaybackEngineLookup
		)
	}
	private val libraryProvider by lazy { LibraryRepository(this) }
	private val applicationNavigation by lazy {
		val intentBuilder = IntentBuilder(this)
		ServerSettingsLaunchingNavigation(
			ActivityApplicationNavigation(
                this,
                intentBuilder,
                BrowserLibrarySelection(
                    applicationSettingsRepository,
                    applicationMessageBus,
                    libraryProvider,
                ),
			),
			intentBuilder,
		)
	}
	private val applicationMessageBus by lazyScoped { getApplicationMessageBus().getScopedMessageBus() }
	private val viewModel by buildViewModelLazily {
		ApplicationSettingsViewModel(
			applicationSettingsRepository,
			selectedPlaybackEngineTypeAccess,
			libraryProvider,
			applicationMessageBus,
			SyncScheduler(this),
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Ensure that this task is only started when it's the task root. A workaround for an Android bug.
		// See http://stackoverflow.com/a/7748416
		val intent = intent
		if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
			if (!isTaskRoot) {
				val className = javaClass.name
				logger.info("$className is not the root.  Finishing $className instead of launching.")
				finish()
				return
			}

			applicationSettingsRepository
				.promiseApplicationSettings()
				.then { s ->
					if (s.chosenLibraryId > -1)
						applicationNavigation.viewLibrary(LibraryId(s.chosenLibraryId))
				}
		}

		setContent {
			ProjectBlueTheme {
				ApplicationSettingsView(
					viewModel,
					applicationNavigation,
					PlaybackServiceController(this)
				)
			}
		}

		viewModel.loadSettings()
	}

	private inner class ServerSettingsLaunchingNavigation(
		inner: NavigateApplication,
		private val intentBuilder: BuildIntents,
	) : NavigateApplication by inner, ActivityResultCallback<ActivityResult> {

		private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)
		private val handler by lazy { Handler(mainLooper) }

		override fun viewNewServerSettings() = loopInOperation {
			launcher.launch(intentBuilder.buildNewLibraryIntent())
		}

		override fun viewServerSettings(libraryId: LibraryId) = loopInOperation {
			launcher.launch(intentBuilder.buildLibrarySettingsIntent(libraryId))
		}

		override fun onActivityResult(result: ActivityResult) {
			viewModel.loadSettings()
		}

		private fun <T> loopInOperation(operation: MessageWriter<T>) = LoopedInPromise(operation, handler)
	}
}
