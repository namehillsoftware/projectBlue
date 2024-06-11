package com.lasthopesoftware.bluewater.client.browsing.files.details

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.ApplicationContextAttachedApplicationDependencies.applicationDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.list.ConnectionLostView
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.LibraryIdProviderViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostExceptionFilter
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionUpdatesView
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.shared.MagicPropertyBuilder
import com.lasthopesoftware.bluewater.shared.android.intents.getIntent
import com.lasthopesoftware.bluewater.shared.android.intents.safelyGetParcelableExtra
import com.lasthopesoftware.bluewater.shared.android.ui.ProjectBlueComposableApplication
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.cls
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.promises.extensions.suspend
import com.lasthopesoftware.resources.strings.StringResources
import java.io.IOException

@UnstableApi class FileDetailsActivity : ComponentActivity() {

	companion object {

		private val magicPropertyBuilder by lazy { MagicPropertyBuilder(cls<FileDetailsActivity>()) }
		val playlist by lazy { magicPropertyBuilder.buildProperty("playlist") }
		val playlistPosition by lazy { magicPropertyBuilder.buildProperty("playlistPosition") }
		val libraryIdKey by lazy { magicPropertyBuilder.buildProperty("libraryId") }

		fun Context.launchFileDetailsActivity(libraryId: LibraryId, playlist: Collection<ServiceFile>, position: Int) {
			startActivity(getIntent<FileDetailsActivity>().apply {
				putExtra(playlistPosition, position)
				putExtra(Companion.playlist, playlist.map { it.key }.toIntArray())
				putExtra(libraryIdKey, libraryId)
			})
		}
	}

	private val imageProvider by lazy { CachedImageProvider.getInstance(this) }

	private val defaultImageProvider by lazy { DefaultImageProvider(this) }

	private val selectedLibraryIdProvider by buildViewModelLazily { LibraryIdProviderViewModel() }

	private val libraryConnections by lazy { buildNewConnectionSessionManager() }

	private val libraryRevisionProvider by lazy { LibraryRevisionProvider(libraryConnections) }

	private val filePropertiesProvider by lazy {
		EditableLibraryFilePropertiesProvider(
			FilePropertiesProvider(
				GuaranteedLibraryConnectionProvider(libraryConnections),
				libraryRevisionProvider,
				FilePropertyCache
			)
		)
	}

	private val filePropertyUpdates by lazy {
		FilePropertyStorage(
			libraryConnections,
			ConnectionAuthenticationChecker(libraryConnections),
			libraryRevisionProvider,
			FilePropertyCache,
			ApplicationMessageBus.getApplicationMessageBus(),
		)
	}

	private val connectionPermissions by lazy { ConnectionAuthenticationChecker(libraryConnections) }

	private val vm by buildViewModelLazily {
		FileDetailsViewModel(
			connectionPermissions,
			filePropertiesProvider,
			filePropertyUpdates,
			defaultImageProvider,
			imageProvider,
			PlaybackServiceController(this),
			ApplicationMessageBus.getApplicationMessageBus(),
			UrlKeyProvider(libraryConnections),
		)
	}

	private val connectionStatusViewModel by lazy {
		val applicationNavigation = ActivityApplicationNavigation(this, applicationDependencies.intentBuilder)

		ConnectionStatusViewModel(
			StringResources(this),
			DramaticConnectionInitializationController(
				libraryConnections,
				applicationNavigation,
            ),
		)
	}

	private val activityApplicationNavigation by lazy {
		ActivityApplicationNavigation(this, applicationDependencies.intentBuilder)
	}

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val libraryId = intent.safelyGetParcelableExtra<LibraryId>(libraryIdKey)

		if (libraryId == null) {
			finish()
			return
		}

		selectedLibraryIdProvider.selectLibraryId(libraryId)

		setContent {
			ProjectBlueComposableApplication {
				val isGettingConnection by connectionStatusViewModel.isGettingConnection.collectAsState()
				var isConnectionLost by remember { mutableStateOf(false) }

				when {
					isGettingConnection -> {
						ConnectionUpdatesView(connectionViewModel = connectionStatusViewModel)
					}
					isConnectionLost -> {
						ConnectionLostView(onCancel = { finish() }, onRetry = { isConnectionLost = false })
					}
					else -> {
						FileDetailsView(vm, activityApplicationNavigation)
					}
				}

				if (!isConnectionLost) {
					LaunchedEffect(key1 = Unit) {
						try {
							val isInitialized = connectionStatusViewModel.initializeConnection(libraryId).suspend()
							if (!isInitialized) {
								isConnectionLost = true
								return@LaunchedEffect
							}

							val position = intent.getIntExtra(playlistPosition, -1)
							if (position < 0) {
								finish()
								return@LaunchedEffect
							}
							val playlist = intent.getIntArrayExtra(playlist)?.map(::ServiceFile) ?: emptyList()

							vm.loadFromList(libraryId, playlist, position).suspend()
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

