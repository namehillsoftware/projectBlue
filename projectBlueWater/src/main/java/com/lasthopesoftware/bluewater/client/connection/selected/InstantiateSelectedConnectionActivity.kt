package com.lasthopesoftware.bluewater.client.connection.selected

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.initialization.*
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.extensions.*
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class InstantiateSelectedConnectionActivity : AppCompatActivity(), ControlConnectionInitialization {

	private val handler by lazy { Handler(mainLooper) }

	private val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	private val connectionInitializationProxy by lazy {
		DramaticConnectionInitializationProxy(
			libraryConnectionProvider,
		)
	}

	private val selectedLibraryIdProvider by lazy { getCachedSelectedLibraryIdProvider() }

	private val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			this,
			IntentBuilder(this),
		)
	}

	private val errorController by lazy {
		ConnectionInitializationErrorController(this, applicationNavigation)
	}

	private val connectionStatusViewModel by buildViewModelLazily {
		ConnectionStatusViewModel(
			StringResources(this),
			errorController,
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
					// If no library is chosen, view application settings
					?: applicationNavigation.viewApplicationSettings()
			}
			.inevitably(LoopedInPromise.act(::finish, handler))

		onBackPressedDispatcher.addCallback {
			with (connectionStatusViewModel) {
				if (isGettingConnection.value)
					cancelCurrentCheck()
			}
		}
	}

	override fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>(), PromisedResponse<IConnectionProvider?, Unit> {
			init {
				val promisedConnection = connectionInitializationProxy.promiseInitializedConnection(libraryId)
				proxyRejection(promisedConnection)
				promisedConnection.eventually(this)
			}

			override fun promiseResponse(connection: IConnectionProvider?): Promise<Unit> {
				if (connection != null && intent?.action != START_ACTIVITY_FOR_RETURN) {
					return selectedLibraryIdProvider
						.promiseSelectedLibraryId()
						.eventually {
							it?.let(applicationNavigation::viewLibrary).keepPromise(Unit)
						}
						.then({ resolve(connection) }, ::reject)
				}

				resolve(connection)
				return Unit.toPromise()
			}
		}

	companion object {
		private val START_ACTIVITY_FOR_RETURN = MagicPropertyBuilder.buildMagicPropertyName<InstantiateSelectedConnectionActivity>("START_ACTIVITY_FOR_RETURN")

		fun restoreSelectedConnection(activity: ComponentActivity): Promise<ActivityResult?> =
			getInstance(activity).isSessionConnectionActive().eventually { isActive ->
				if (!isActive) activity.promiseActivityResult(
					Intent(activity, cls<InstantiateSelectedConnectionActivity>()).apply {
						action = START_ACTIVITY_FOR_RETURN
					}
				)
				else Promise.empty()
			}
	}
}
