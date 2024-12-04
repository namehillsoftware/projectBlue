package com.lasthopesoftware.bluewater.client.browsing

import androidx.lifecycle.ViewModelStoreOwner
import com.lasthopesoftware.bluewater.client.browsing.files.details.FileDetailsViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.properties.EditableLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.settings.PermissionsDependencies
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily

class ScopedViewModelRegistry(
	reusedViewModelDependencies: ReusedViewModelDependencies,
	permissionsDependencies: PermissionsDependencies,
	viewModelStoreOwner: ViewModelStoreOwner
) :
	ScopedViewModelDependencies,
	ReusedViewModelDependencies by reusedViewModelDependencies
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

	override val fileDetailsViewModel by viewModelStoreOwner.buildViewModelLazily {
		FileDetailsViewModel(
			connectionPermissions = ConnectionAuthenticationChecker(libraryConnectionProvider),
			filePropertiesProvider = EditableLibraryFilePropertiesProvider(freshLibraryFileProperties),
			updateFileProperties = filePropertiesStorage,
			defaultImageProvider = defaultImageProvider,
			imageProvider = imageBytesProvider,
			controlPlayback = playbackServiceController,
			registerForApplicationMessages = registerForApplicationMessages,
			urlKeyProvider = urlKeyProvider,
		)
	}
}
