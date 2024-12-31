package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.libraries.RetryingConnectionApplicationDependencies
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.ui.ProjectBlueComposableApplication
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import java.io.IOException

@UnstableApi class FileDetailsActivity : ComponentActivity() {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<FileDetailsActivity>()) }
		val playlist by lazy { magicPropertyBuilder.buildProperty("playlist") }
		val playlistPosition by lazy { magicPropertyBuilder.buildProperty("playlistPosition") }
		val libraryIdKey by lazy { magicPropertyBuilder.buildProperty("libraryId") }
	}

	private val localApplicationDependencies by lazy { RetryingConnectionApplicationDependencies(applicationDependencies) }

	private val libraryConnectedDependencies by lazy {
		LibraryConnectionRegistry(localApplicationDependencies)
	}

	private val filePropertiesProvider by lazy {
		EditableLibraryFilePropertiesProvider(libraryConnectedDependencies.freshLibraryFileProperties)
	}

	private val vm by buildViewModelLazily {
		FileDetailsViewModel(
			libraryConnectedDependencies.connectionAuthenticationChecker,
			filePropertiesProvider,
			libraryConnectedDependencies.filePropertiesStorage,
			localApplicationDependencies.defaultImageProvider,
			libraryConnectedDependencies.imageBytesProvider,
			localApplicationDependencies.playbackServiceController,
			localApplicationDependencies.registerForApplicationMessages,
			libraryConnectedDependencies.urlKeyProvider,
		)
	}

	private val activityApplicationNavigation by lazy {
		ActivityApplicationNavigation(this, localApplicationDependencies.intentBuilder)
	}

	private val connectionStatusViewModel by buildViewModelLazily {
		ConnectionStatusViewModel(
			localApplicationDependencies.stringResources,
			DramaticConnectionInitializationController(
				localApplicationDependencies.connectionSessions,
				activityApplicationNavigation,
            ),
		)
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val libraryId = intent.safelyGetParcelableExtra<LibraryId>(libraryIdKey)

		if (libraryId == null) {
			finish()
			return
		}

		WindowCompat.setDecorFitsSystemWindows(window, false)

		setContent {
			ProjectBlueComposableApplication {
				val isGettingConnection by connectionStatusViewModel.isGettingConnection.subscribeAsState()
				var isConnectionLost by remember { mutableStateOf(false) }

				when {
					isGettingConnection -> ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
					isConnectionLost -> ConnectionLostView(onCancel = { finish() }, onRetry = { isConnectionLost = false })
					else -> FileDetailsView(vm, activityApplicationNavigation)
				}

				if (!isConnectionLost) {
					LaunchedEffect(key1 = Unit) {
						try {
							val isInitialized = connectionStatusViewModel.initializeConnection(libraryId).suspend()
							val position = intent.getIntExtra(playlistPosition, -1)
							when {
								position < 0 -> finish()
								!isInitialized -> isConnectionLost = true
								else -> {
									val playlist = intent.getIntArrayExtra(playlist)?.map(::ServiceFile) ?: emptyList()

									vm.loadFromList(libraryId, playlist, position).suspend()
								}
							}
						} catch (e: IOException) {
							if (ConnectionLostExceptionFilter.isConnectionLostException(e))
								isConnectionLost = true
							else
								finish()
						} catch (e: Throwable) {
							finish()
						}
					}
				}
			}
		}
	}
}

