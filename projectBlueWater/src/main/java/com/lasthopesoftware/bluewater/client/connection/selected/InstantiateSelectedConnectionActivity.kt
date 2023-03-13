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
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.BuildingConnectionStatus
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.selected.SelectedConnection.Companion.getInstance
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.initialization.*
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.promises.PromiseDelay
import com.lasthopesoftware.bluewater.shared.promises.extensions.*
import com.lasthopesoftware.resources.strings.StringResources
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.response.PromisedResponse

class InstantiateSelectedConnectionActivity : AppCompatActivity(), ControlConnectionInitialization {

	private val handler by lazy { Handler(mainLooper) }

	private val libraryConnectionProvider by lazy { buildNewConnectionSessionManager() }

	private val connectionInitializationProxy by lazy { ConnectionInitializationProxy(libraryConnectionProvider) }

	private val applicationNavigation by lazy { ActivityApplicationNavigation(this) }

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
					.keepPromise(false)
			}
			.must(::finishForResultDelayed)

		onBackPressedDispatcher.addCallback {
			with (connectionStatusViewModel) {
				if (isGettingConnection.value)
					cancelCurrentCheck()
				finish()
			}
		}
	}

	override fun promiseInitializedConnection(libraryId: LibraryId): ProgressingPromise<BuildingConnectionStatus, IConnectionProvider?> =
		object : ProgressingPromiseProxy<BuildingConnectionStatus, IConnectionProvider?>(), PromisedResponse<IConnectionProvider?, Unit> {
			init {
				val promisedConnection = connectionInitializationProxy.promiseInitializedConnection(libraryId)
				doCancel(promisedConnection)
				proxyRejection(promisedConnection)
				promisedConnection.eventually(this)
			}

			override fun promiseResponse(connection: IConnectionProvider?): Promise<Unit> =
				if (connection != null) {
					if (intent?.action == START_ACTIVITY_FOR_RETURN) finishForResultDelayed().also(::doCancel)
					else PromiseDelay
						.delay<Any?>(ConnectionInitializationConstants.dramaticPause)
						.also(::doCancel)
						.guaranteedUnitResponse()
						.eventually { applicationNavigation.viewBrowserRoot() }
				} else finishForResultDelayed().also(::doCancel)
		}

	private fun finishForResultDelayed() = CancellableProxyPromise { cp ->
		PromiseDelay
			.delay<Any?>(ConnectionInitializationConstants.dramaticPause)
			.also(cp::doCancel)
			.guaranteedUnitResponse()
			.eventually(LoopedInPromise.response({ finish() }, handler))
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

		fun startNewConnection(context: Context) {
			context.startActivity(Intent(context, cls<InstantiateSelectedConnectionActivity>()))
		}
	}
}
