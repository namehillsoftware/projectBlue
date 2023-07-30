package com.lasthopesoftware.bluewater.client.browsing

import androidx.lifecycle.ViewModelStoreOwner
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily

class ScopedViewModelDependencies(inner: BrowserViewDependencies, viewModelStoreOwner: ViewModelStoreOwner) : ScopedBrowserViewDependencies, BrowserViewDependencies by inner {

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
			applicationPermissions = applicationPermissions,
		)
	}
}
