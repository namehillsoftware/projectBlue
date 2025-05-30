package com.lasthopesoftware.bluewater.client.browsing

import androidx.lifecycle.ViewModelStoreOwner
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependents
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistsStorage
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependents
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily

class ReusedViewModelRegistry(
    entryDependencies: EntryDependencies,
    libraryConnectionDependents: LibraryConnectionDependents,
	libraryFilePropertiesDependents: LibraryFilePropertiesDependents,
    viewModelStoreOwner: ViewModelStoreOwner
) :
	ReusedViewModelDependencies,
	EntryDependencies by entryDependencies,
	LibraryConnectionDependents by libraryConnectionDependents,
	LibraryFilePropertiesDependents by libraryFilePropertiesDependents
{
	override val nowPlayingCoverArtViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingCoverArtViewModel(
			registerForApplicationMessages,
			nowPlayingState,
			urlKeyProvider,
			defaultImageProvider,
			imageBytesProvider,
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

	override val nowPlayingScreenViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingScreenViewModel(
			registerForApplicationMessages,
			nowPlayingViewModelMessageBus,
			nowPlayingDisplaySettings,
			playbackServiceController,
		)
	}

	override val nowPlayingFilePropertiesViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			registerForApplicationMessages,
			nowPlayingState,
			freshLibraryFileProperties,
			urlKeyProvider,
			filePropertiesStorage,
			connectionAuthenticationChecker,
			playbackServiceController,
			pollForConnections,
			stringResources,
			nowPlayingViewModelMessageBus,
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
			),
			registerForApplicationMessages,
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
