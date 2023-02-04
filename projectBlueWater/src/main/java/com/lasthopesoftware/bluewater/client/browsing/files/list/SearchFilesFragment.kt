package com.lasthopesoftware.bluewater.client.browsing.files.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.lasthopesoftware.bluewater.ActivityApplicationNavigation
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.RateControlledFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.SelectedLibraryFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.SelectedLibraryUrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.ConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.activity.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.ui.theme.ProjectBlueTheme
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildActivityViewModelLazily
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus.Companion.getApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.bluewater.shared.policies.ratelimiting.PromisingRateLimiter
import com.lasthopesoftware.resources.closables.LifecycleCloseableManager
import com.lasthopesoftware.resources.strings.StringResources

class SearchFilesFragment : Fragment() {

	private val selectedLibraryIdProvider by lazy { requireContext().getCachedSelectedLibraryIdProvider() }

	private val libraryConnectionProvider by lazy { ConnectionSessionManager.get(requireContext()) }

	private val fileProvider by lazy {
		val stringListProvider = LibraryFileStringListProvider(libraryConnectionProvider)
		LibraryFileProvider(stringListProvider)
	}

	private val scopedUrlKeyProvider by lazy {
		SelectedLibraryUrlKeyProvider(
			selectedLibraryIdProvider,
			UrlKeyProvider(libraryConnectionProvider)
		)
	}

	private val lifecycleCloseableManager by lazy { LifecycleCloseableManager(this) }

	private val scopedMessageBus by lazy {
		getApplicationMessageBus().getScopedMessageBus().also(lifecycleCloseableManager::manage)
	}

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			RateControlledFilePropertiesProvider(
				FilePropertiesProvider(
					libraryConnectionProvider,
					revisionProvider,
					FilePropertyCache,
				),
				PromisingRateLimiter(1),
			),
		)
	}

	private val searchFilesViewModel by buildViewModelLazily {
		SearchFilesViewModel(
			selectedLibraryIdProvider,
			fileProvider,
			PlaybackServiceController(requireContext()),
		)
	}

	private val scopedFilePropertiesProvider by lazy {
		SelectedLibraryFilePropertiesProvider(
			selectedLibraryIdProvider,
			libraryFilePropertiesProvider,
		)
	}

	private val menuMessageBus by buildActivityViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	private val itemListMenuBackPressedHandler by lazy { ItemListMenuBackPressedHandler(menuMessageBus) }

	private val trackHeadlineViewModelProvider by buildViewModelLazily {
		TrackHeadlineViewModelProvider(
			scopedFilePropertiesProvider,
			scopedUrlKeyProvider,
			StringResources(requireContext()),
			PlaybackServiceController(requireContext()),
			ActivityApplicationNavigation(requireActivity()),
			menuMessageBus,
			scopedMessageBus,
		)
	}

	private val connectionAuthenticationChecker by lazy {
		ConnectionAuthenticationChecker(libraryConnectionProvider)
	}

	private val filePropertiesStorage by lazy {
		FilePropertyStorage(
			libraryConnectionProvider,
			connectionAuthenticationChecker,
			revisionProvider,
			FilePropertyCache,
			scopedMessageBus
		)
	}

	private val nowPlayingFilePropertiesViewModel by buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			scopedMessageBus,
			LiveNowPlayingLookup.getInstance(),
			libraryFilePropertiesProvider,
			UrlKeyProvider(libraryConnectionProvider),
			filePropertiesStorage,
			connectionAuthenticationChecker,
			PlaybackServiceController(requireContext()),
			ConnectionPoller(requireContext()),
			StringResources(requireContext()),
		)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return ComposeView(requireContext()).apply {
			setContent {
				ProjectBlueTheme {
					SearchFilesView(
						searchFilesViewModel = searchFilesViewModel,
						nowPlayingViewModel = nowPlayingFilePropertiesViewModel,
						trackHeadlineViewModelProvider = trackHeadlineViewModelProvider,
						itemListMenuBackPressedHandler = itemListMenuBackPressedHandler,
					)
				}
			}
		}
	}
}
