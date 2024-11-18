package com.lasthopesoftware.bluewater.client.browsing

import androidx.lifecycle.ViewModelStoreOwner
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistsStorage
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependencies
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily

class ScopedViewModelDependencies(
	entryDependencies: EntryDependencies,
	libraryConnectionDependencies: LibraryConnectionDependencies,
	permissionsDependencies: PermissionsDependencies,
	viewModelStoreOwner: ViewModelStoreOwner
) :
	ViewDependencies,
	EntryDependencies by entryDependencies,
	LibraryConnectionDependencies by libraryConnectionDependencies
{
	override val itemListViewModel by viewModelStoreOwner.buildViewModelLazily {
        ItemListViewModel(
            itemProvider,
            registerForApplicationMessages,
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
            registerForApplicationMessages,
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

	override val nowPlayingCoverArtViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingCoverArtViewModel(
			registerForApplicationMessages,
			nowPlayingState,
			libraryConnectionProvider,
			defaultImageProvider,
			imageProvider,
			pollForConnections,
		)
	}

	override val nowPlayingPlaylistViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingPlaylistViewModel(
			registerForApplicationMessages,
			nowPlayingState,
			playbackServiceController,
			PlaylistsStorage(libraryConnectionProvider),
		)
	}

	override val nowPlayingViewModelMessageBus by viewModelStoreOwner.buildViewModelLazily {
		ViewModelMessageBus<NowPlayingMessage>()
	}

	override val fileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		FileDetailsViewModel(
			connectionPermissions = ConnectionAuthenticationChecker(libraryConnectionProvider),
			filePropertiesProvider = EditableLibraryFilePropertiesProvider(libraryFilePropertiesProvider),
			updateFileProperties = filePropertiesStorage,
			defaultImageProvider = defaultImageProvider,
			imageProvider = imageProvider,
			controlPlayback = playbackServiceController,
			registerForApplicationMessages = registerForApplicationMessages,
			urlKeyProvider = urlKeyProvider,
		)
	}

	override val nowPlayingScreenViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingScreenViewModel(
			registerForApplicationMessages,
			InMemoryNowPlayingDisplaySettings,
			playbackServiceController,
		)
	}

	override val nowPlayingFilePropertiesViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			registerForApplicationMessages,
			nowPlayingState,
			freshLibraryFileProperties,
			UrlKeyProvider(libraryConnectionProvider),
			filePropertiesStorage,
			connectionAuthenticationChecker,
			playbackServiceController,
			pollForConnections,
			stringResources,
		)
	}

	override val reusablePlaylistFileItemViewModelProvider by viewModelStoreOwner.buildViewModelLazily {
		ReusablePlaylistFileItemViewModelProvider(
			libraryFilePropertiesProvider,
			urlKeyProvider,
			stringResources,
			menuMessageBus,
			registerForApplicationMessages,
		)
	}

	override val reusableFileItemViewModelProvider by viewModelStoreOwner.buildViewModelLazily {
		ReusableFileItemViewModelProvider(
			libraryFilePropertiesProvider,
			urlKeyProvider,
			stringResources,
			registerForApplicationMessages,
		)
	}

	override val connectionStatusViewModel by viewModelStoreOwner.buildViewModelLazily {
		ConnectionStatusViewModel(
			stringResources,
			DramaticConnectionInitializationController(
				connectionSessions,
				applicationNavigation,
			),
		)
	}

	override val connectionWatcherViewModel by viewModelStoreOwner.buildViewModelLazily {
		ConnectionWatcherViewModel(
			registerForApplicationMessages,
			libraryConnectionProvider,
			pollForConnections,
		)
	}
}
