package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.ApplicationDependenciesContainer.applicationDependencies
import com.lasthopesoftware.bluewater.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.android.ui.ProjectBlueComposableApplication
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinitionProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependentsRegistry
import com.lasthopesoftware.bluewater.client.browsing.items.KeyedIdentifier
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionRegistry
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.observables.subscribeAsState
import com.lasthopesoftware.promises.extensions.suspend
import java.io.IOException

@UnstableApi class FileDetailsActivity : ComponentActivity() {

	companion object {
		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<FileDetailsActivity>()) }
		val playlistPosition by lazy { magicPropertyBuilder.buildProperty("playlistPosition") }
		val itemId by lazy { magicPropertyBuilder.buildProperty("itemId") }
		val searchQuery by lazy { magicPropertyBuilder.buildProperty("searchQuery") }
		val positionedFile by lazy { magicPropertyBuilder.buildProperty("positionedFile") }
		val libraryIdKey by lazy { magicPropertyBuilder.buildProperty("libraryId") }
	}

	private val localApplicationDependencies by lazy { applicationDependencies }

	private val libraryConnectedDependencies by lazy {
		LibraryConnectionRegistry(localApplicationDependencies)
	}

	private val filePropertiesProvider by lazy {
		EditableLibraryFilePropertiesProvider(
			libraryConnectedDependencies.freshLibraryFileProperties,
			EditableFilePropertyDefinitionProvider(localApplicationDependencies.libraryConnectionProvider),
		)
	}

	private val libraryFilePropertiesDependents by lazy {
		LibraryFilePropertiesDependentsRegistry(localApplicationDependencies, libraryConnectedDependencies)
	}

	private val vm by buildViewModelLazily {
		FileDetailsFromItemViewModel(
			libraryConnectedDependencies.connectionAuthenticationChecker,
			filePropertiesProvider,
			libraryConnectedDependencies.filePropertiesStorage,
			localApplicationDependencies.defaultImageProvider,
			libraryFilePropertiesDependents.imageBytesProvider,
			localApplicationDependencies.playbackServiceController,
			localApplicationDependencies.registerForApplicationMessages,
			libraryConnectedDependencies.urlKeyProvider,
			libraryConnectedDependencies.libraryFilesProvider,
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
            ),
			localApplicationDependencies.registerForApplicationMessages,
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
		enableEdgeToEdge()

		setContent {
			ProjectBlueComposableApplication {
				val isGettingConnection by connectionStatusViewModel.isGettingConnection.subscribeAsState()
				var isConnectionLost by remember { mutableStateOf(false) }

				when {
					isGettingConnection -> ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
					isConnectionLost -> ConnectionLostView(onCancel = { finish() }, onRetry = { isConnectionLost = false })
					else -> FileDetailsView(vm, activityApplicationNavigation, localApplicationDependencies.bitmapProducer)
				}

				if (!isConnectionLost) {
					LaunchedEffect(key1 = Unit) {
						try {
							val isInitialized = connectionStatusViewModel.initializeConnection(libraryId).suspend()
							val keyedIdentifier = intent.safelyGetParcelableExtra<KeyedIdentifier>(itemId)
							val positionedFile = intent.safelyGetParcelableExtra<PositionedFile>(positionedFile)
							when {
								positionedFile == null -> finish()
								!isInitialized -> isConnectionLost = true
								keyedIdentifier != null -> vm.load(libraryId, keyedIdentifier, positionedFile).suspend()
//								query != null -> vm.load(libraryId, query, positionedFile).suspend()
							}
						} catch (e: IOException) {
							if (ConnectionLostExceptionFilter.isConnectionLostException(e))
								isConnectionLost = true
							else
								finish()
						} catch (_: Throwable) {
							finish()
						}
					}
				}
			}
		}
	}
}

