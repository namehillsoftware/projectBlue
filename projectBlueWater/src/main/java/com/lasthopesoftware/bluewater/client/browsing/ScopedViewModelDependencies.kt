package com.lasthopesoftware.bluewater.client.browsing

import androidx.lifecycle.ViewModelStoreOwner
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily

class ScopedViewModelDependencies(inner: BrowserViewDependencies, permissionsDependencies: PermissionsDependencies, viewModelStoreOwner: ViewModelStoreOwner) : ScopedBrowserViewDependencies, BrowserViewDependencies by inner {

	override val itemListViewModel by viewModelStoreOwner.buildViewModelLazily {
        ItemListViewModel(
            itemProvider,
            messageBus,
            libraryProvider,
        )
	}

	override val fileListViewModel by viewModelStoreOwner.buildViewModelLazily {
        FileListViewModel(
            itemFileProvider,
            storedItemAccess,
        )
	}

	override val activeFileDownloadsViewModel by viewModelStoreOwner.buildViewModelLazily {
        ActiveFileDownloadsViewModel(
            storedFileAccess,
            messageBus,
            syncScheduler,
        )
	}

	override val searchFilesViewModel by viewModelStoreOwner.buildViewModelLazily {
        SearchFilesViewModel(libraryFilesProvider)
	}

	override val librarySettingsViewModel by viewModelStoreOwner.buildViewModelLazily {
		LibrarySettingsViewModel(
			libraryProvider = libraryProvider,
			libraryStorage = libraryStorage,
			libraryRemoval = libraryRemoval,
			applicationPermissions = permissionsDependencies.applicationPermissions,
		)
	}

	override val nowPlayingViewModelMessageBus by viewModelStoreOwner.buildViewModelLazily {
		ViewModelMessageBus<NowPlayingMessage>()
	}

	override val fileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		FileDetailsViewModel(
			connectionPermissions = ConnectionAuthenticationChecker(libraryConnectionProvider),
			filePropertiesProvider = EditableLibraryFilePropertiesProvider(
				FilePropertiesProvider(
					GuaranteedLibraryConnectionProvider(libraryConnectionProvider),
					revisionProvider,
					FilePropertyCache,
				)
			),
			updateFileProperties = filePropertiesStorage,
			defaultImageProvider = defaultImageProvider,
			imageProvider = imageProvider,
			controlPlayback = playbackServiceController,
			registerForApplicationMessages = messageBus,
			urlKeyProvider = urlKeyProvider,
		)
	}

	override val nowPlayingScreenViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingScreenViewModel(
			messageBus,
			InMemoryNowPlayingDisplaySettings,
			playbackServiceController,
		)
	}
}
