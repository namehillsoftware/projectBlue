package com.lasthopesoftware.bluewater.client.browsing

import androidx.lifecycle.ViewModelStoreOwner
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableFilePropertyDefinitionProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
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
			libraryNameLookup = libraryNameLookup,
			librarySettingsProvider = librarySettingsProvider,
			librarySettingsStorage = librarySettingsStorage,
			libraryRemoval = libraryRemoval,
			applicationPermissions = permissionsDependencies.applicationPermissions,
			stringResources = stringResources,
		)
	}

	override val fileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		FileDetailsViewModel(
			connectionPermissions = connectionAuthenticationChecker,
			filePropertiesProvider = EditableLibraryFilePropertiesProvider(
				freshLibraryFileProperties,
				EditableFilePropertyDefinitionProvider(libraryConnectionProvider),
			),
			updateFileProperties = filePropertiesStorage,
			defaultImageProvider = defaultImageProvider,
			imageProvider = imageBytesProvider,
			controlPlayback = playbackServiceController,
			registerForApplicationMessages = registerForApplicationMessages,
			urlKeyProvider = urlKeyProvider,
		)
	}

	override val undoBackStackBuilder by viewModelStoreOwner.buildViewModelLazily { ViewModelUndoStack() }

	override val applicationNavigation by lazy {
		UndoStackApplicationNavigation(undoBackStackBuilder, reusedViewModelDependencies.applicationNavigation)
	}
}
