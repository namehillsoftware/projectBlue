package com.lasthopesoftware.bluewater.client.browsing

import androidx.lifecycle.ViewModelStoreOwner
import com.lasthopesoftware.bluewater.client.browsing.files.details.BrowsedFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.NowPlayingFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.details.SearchedFileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.search.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.AggregateItemViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.shared.android.UndoStackApplicationNavigation
import com.lasthopesoftware.bluewater.shared.android.ViewModelUndoStack
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily

class ScopedViewModelRegistry(
	reusedViewModelDependencies: ReusedViewModelDependencies,
	permissionsDependencies: PermissionsDependencies,
	viewModelStoreOwner: ViewModelStoreOwner,
) :
	ScopedViewModelDependencies,
	ReusedViewModelDependencies by reusedViewModelDependencies
{
	override val itemListViewModel by viewModelStoreOwner.buildViewModelLazily {
        ItemListViewModel(
			itemProvider,
			registerForApplicationMessages,
			libraryNameLookup,
        )
	}

	override val fileListViewModel by viewModelStoreOwner.buildViewModelLazily {
        FileListViewModel(
            libraryFilesProvider,
            storedItemAccess,
        )
	}

	override val itemDataLoader by viewModelStoreOwner.buildViewModelLazily {
		AggregateItemViewModel(
			itemListViewModel,
			fileListViewModel
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
            librarySettingsProvider = librarySettingsProvider,
			librarySettingsStorage = librarySettingsStorage,
			libraryRemoval = libraryRemoval,
			applicationPermissions = permissionsDependencies.applicationPermissions,
			manageConnectionSessions = connectionSessions,
			stringResources = stringResources,
		)
	}

	override val fileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		FileDetailsViewModel(
			connectionPermissions = connectionAuthenticationChecker,
			filePropertiesProvider = freshLibraryFileProperties,
			updateFileProperties = filePropertiesStorage,
			defaultImageProvider = defaultImageProvider,
			imageProvider = imageBytesProvider,
			controlPlayback = playbackServiceController,
			registerForApplicationMessages = registerForApplicationMessages,
			urlKeyProvider = urlKeyProvider,
		)
	}

	override val browsedFileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		BrowsedFileDetailsViewModel(
			playbackServiceController,
			fileDetailsViewModel,
			fileDetailsViewModel,
			libraryFilesProvider,
		)
	}
	override val searchedFileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		SearchedFileDetailsViewModel(
			playbackServiceController,
			fileDetailsViewModel,
			fileDetailsViewModel,
			libraryFilesProvider,
		)
	}

	override val nowPlayingFileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		NowPlayingFileDetailsViewModel(
			playbackServiceController,
			fileDetailsViewModel,
			fileDetailsViewModel,
			nowPlayingState,
			registerForApplicationMessages,
		)
	}

	override val undoBackStackBuilder by viewModelStoreOwner.buildViewModelLazily { ViewModelUndoStack() }

	override val applicationNavigation by lazy {
		UndoStackApplicationNavigation(undoBackStackBuilder, reusedViewModelDependencies.applicationNavigation)
	}
}
