package com.lasthopesoftware.bluewater

import androidx.activity.ComponentActivity
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.client.ActivitySuppliedDependencies
import com.lasthopesoftware.bluewater.client.browsing.BrowserViewDependencies
import com.lasthopesoftware.bluewater.client.browsing.files.access.CachedItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.DelegatingItemFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.parameters.FileListParameters
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.ItemStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.stringlist.LibraryFileStringListProvider
import com.lasthopesoftware.bluewater.client.browsing.files.image.CachedImageProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.CachedFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.DelegatingFilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.FilePropertiesProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.repository.FilePropertyCache
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.FilePropertyStorage
import com.lasthopesoftware.bluewater.client.browsing.items.access.CachedItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.access.DelegatingItemProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemPlayback
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.ItemListMenuMessage
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.items.playlists.PlaylistsStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRemoval
import com.lasthopesoftware.bluewater.client.browsing.library.access.LibraryRepository
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.BrowserLibrarySelection
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.CachedSelectedLibraryIdProvider.Companion.getCachedSelectedLibraryIdProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryViewModel
import com.lasthopesoftware.bluewater.client.browsing.library.revisions.LibraryRevisionProvider
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.connection.ConnectionLostRetryHandler
import com.lasthopesoftware.bluewater.client.connection.authentication.ConnectionAuthenticationChecker
import com.lasthopesoftware.bluewater.client.connection.libraries.GuaranteedLibraryConnectionProvider
import com.lasthopesoftware.bluewater.client.connection.libraries.UrlKeyProvider
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPoller
import com.lasthopesoftware.bluewater.client.connection.polling.LibraryConnectionPollingSessions
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionSessionManager.Instance.buildNewConnectionSessionManager
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.DramaticConnectionInitializationController
import com.lasthopesoftware.bluewater.client.connection.settings.ConnectionSettingsLookup
import com.lasthopesoftware.bluewater.client.connection.settings.changes.ObservableConnectionSettingsLibraryStorage
import com.lasthopesoftware.bluewater.client.connection.trust.UserSslCertificateProvider
import com.lasthopesoftware.bluewater.client.playback.engine.selection.SelectedPlaybackEngineTypeAccess
import com.lasthopesoftware.bluewater.client.playback.engine.selection.defaults.DefaultPlaybackEngineLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.LiveNowPlayingLookup
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.InMemoryNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsViewModel
import com.lasthopesoftware.bluewater.settings.repository.access.CachingApplicationSettingsRepository.Companion.getApplicationSettingsRepository
import com.lasthopesoftware.bluewater.shared.android.intents.IntentBuilder
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus
import com.lasthopesoftware.bluewater.shared.android.viewmodels.buildViewModelLazily
import com.lasthopesoftware.bluewater.shared.images.DefaultImageProvider
import com.lasthopesoftware.bluewater.shared.messages.application.ApplicationMessageBus
import com.lasthopesoftware.bluewater.shared.messages.application.getScopedMessageBus
import com.lasthopesoftware.policies.retries.RateLimitingExecutionPolicy
import com.lasthopesoftware.policies.retries.RetryExecutionPolicy
import com.lasthopesoftware.resources.closables.ViewModelCloseableManager
import com.lasthopesoftware.resources.strings.StringResources
import com.lasthopesoftware.resources.uri.DocumentUriSelector

@UnstableApi
class ActivityDependencies(activity: ComponentActivity, activitySuppliedDependencies: ActivitySuppliedDependencies) : BrowserViewDependencies {
	private val applicationContext by lazy { activity.applicationContext }

	private val viewModelScope by activity.buildViewModelLazily { ViewModelCloseableManager() }

	private val libraryFileStringListProvider by lazy { LibraryFileStringListProvider(libraryConnectionProvider) }

	private val connectionAuthenticationChecker by lazy { ConnectionAuthenticationChecker(libraryConnectionProvider) }

	private val revisionProvider by lazy { LibraryRevisionProvider(libraryConnectionProvider) }

	private val filePropertiesStorage by lazy {
		FilePropertyStorage(
			libraryConnectionProvider,
			connectionAuthenticationChecker,
			revisionProvider,
			FilePropertyCache,
			messageBus
		)
	}

	private val itemListProvider by lazy {
		ItemStringListProvider(
			FileListParameters,
			libraryFileStringListProvider)
	}

	private val connectionLostRetryPolicy by lazy { RetryExecutionPolicy(ConnectionLostRetryHandler) }

	private val libraryRepository by lazy { LibraryRepository(applicationContext) }

	private val defaultImageProvider by lazy { DefaultImageProvider(applicationContext) }

	private val imageProvider by lazy { CachedImageProvider.getInstance(applicationContext) }

	private val singleRatePolicy by lazy { RateLimitingExecutionPolicy(1) }

	private val freshRateLimitedLibraryFileProperties by lazy {
		DelegatingFilePropertiesProvider(
			DelegatingFilePropertiesProvider(
				FilePropertiesProvider(
					GuaranteedLibraryConnectionProvider(libraryConnectionProvider),
					revisionProvider,
					FilePropertyCache,
				),
				connectionLostRetryPolicy,
			),
			singleRatePolicy,
		)
	}

	private val libraryFilePropertiesProvider by lazy {
		CachedFilePropertiesProvider(
			libraryConnectionProvider,
			FilePropertyCache,
			freshRateLimitedLibraryFileProperties,
		)
	}

	private val selectedLibraryIdProvider by lazy { activity.getCachedSelectedLibraryIdProvider() }

	private val menuMessageBus by activity.buildViewModelLazily { ViewModelMessageBus<ItemListMenuMessage>() }

	private val selectedPlaybackEngineTypeAccess by lazy {
		SelectedPlaybackEngineTypeAccess(
			applicationSettingsRepository,
			DefaultPlaybackEngineLookup
		)
	}

	private val libraryBrowserSelection by lazy {
		BrowserLibrarySelection(
			applicationSettingsRepository,
			messageBus,
			libraryProvider,
		)
	}

	private val urlKeyProvider by lazy { UrlKeyProvider(libraryConnectionProvider) }

	override val messageBus by lazy { ApplicationMessageBus.getApplicationMessageBus().getScopedMessageBus().also(viewModelScope::manage) }

	override val itemListMenuBackPressedHandler by lazy { ItemListMenuBackPressedHandler(menuMessageBus).also(viewModelScope::manage) }

	override val itemProvider by lazy {
		DelegatingItemProvider(
			CachedItemProvider.getInstance(applicationContext),
			connectionLostRetryPolicy
		)
	}

	override val itemFileProvider by lazy {
		DelegatingItemFileProvider(
			CachedItemFileProvider.getInstance(applicationContext),
			connectionLostRetryPolicy,
		)
	}

	override val libraryConnectionProvider by lazy { activity.buildNewConnectionSessionManager() }

	override val playbackServiceController by lazy { PlaybackServiceController(activity) }

	override val nowPlayingState by lazy { LiveNowPlayingLookup.getInstance() }

	override val nowPlayingFilePropertiesViewModel by activity.buildViewModelLazily {
		NowPlayingFilePropertiesViewModel(
			messageBus,
			nowPlayingState,
			freshRateLimitedLibraryFileProperties,
			UrlKeyProvider(libraryConnectionProvider),
			filePropertiesStorage,
			connectionAuthenticationChecker,
			playbackServiceController,
			pollForConnections,
			stringResources,
		)
	}

	override val storedItemAccess by lazy {
		StateChangeBroadcastingStoredItemAccess(StoredItemAccess(applicationContext), messageBus)
	}

	override val storedFileAccess by lazy { StoredFileAccess(applicationContext) }

	override val stringResources by lazy { StringResources(applicationContext) }

	override val libraryFilesProvider by lazy {
		LibraryFileProvider(LibraryFileStringListProvider(libraryConnectionProvider))
	}

	override val applicationNavigation by lazy {
		ActivityApplicationNavigation(
			activity,
			IntentBuilder(applicationContext),
		)
	}

	override val syncScheduler by lazy { SyncScheduler(applicationContext) }

	override val libraryProvider: ILibraryProvider
		get() = libraryRepository

	override val libraryStorage by lazy {
		ObservableConnectionSettingsLibraryStorage(
			libraryRepository,
			ConnectionSettingsLookup(libraryProvider),
			messageBus
		)
	}

	override val libraryRemoval by lazy {
		LibraryRemoval(
			storedItemAccess,
			libraryRepository,
			selectedLibraryIdProvider,
			libraryRepository,
			BrowserLibrarySelection(applicationSettingsRepository, messageBus, libraryProvider),
		)
	}

	override val navigationMessages by activity.buildViewModelLazily { ViewModelMessageBus<NavigationMessage>() }

	override val applicationSettingsRepository by lazy { applicationContext.getApplicationSettingsRepository() }

	override val playbackLibraryItems by lazy { ItemPlayback(itemListProvider, playbackServiceController) }

	override val selectedLibraryViewModel: SelectedLibraryViewModel by activity.buildViewModelLazily {
		SelectedLibraryViewModel(
			selectedLibraryIdProvider,
			libraryBrowserSelection,
		).apply { loadSelectedLibraryId() }
	}

	override val pollForConnections by lazy {
		LibraryConnectionPollingSessions(LibraryConnectionPoller(libraryConnectionProvider))
	}

	override val nowPlayingCoverArtViewModel by activity.buildViewModelLazily {
		NowPlayingCoverArtViewModel(
			messageBus,
			nowPlayingState,
			libraryConnectionProvider,
			defaultImageProvider,
			imageProvider,
			pollForConnections,
		)
	}

	override val nowPlayingPlaylistViewModel by activity.buildViewModelLazily {
		NowPlayingPlaylistViewModel(
			messageBus,
			nowPlayingState,
			playbackServiceController,
			PlaylistsStorage(libraryConnectionProvider),
		)
	}

	override val nowPlayingScreenViewModel by activity.buildViewModelLazily {
		NowPlayingScreenViewModel(
			messageBus,
			InMemoryNowPlayingDisplaySettings,
			playbackServiceController,
		)
	}

	override val connectionWatcherViewModel by activity.buildViewModelLazily {
		ConnectionWatcherViewModel(
			messageBus,
			libraryConnectionProvider,
			pollForConnections,
		)
	}

	override val reusablePlaylistFileItemViewModelProvider by activity.buildViewModelLazily {
		ReusablePlaylistFileItemViewModelProvider(
			libraryFilePropertiesProvider,
			urlKeyProvider,
			stringResources,
			menuMessageBus,
			messageBus,
		)
	}

	override val reusableChildItemViewModelProvider by activity.buildViewModelLazily {
		ReusableChildItemViewModelProvider(
			storedItemAccess,
			menuMessageBus,
		)
	}

	override val reusableFileItemViewModelProvider by activity.buildViewModelLazily {
		ReusableFileItemViewModelProvider(
			libraryFilePropertiesProvider,
			urlKeyProvider,
			stringResources,
			messageBus,
		)
	}

	override val applicationSettingsViewModel by activity.buildViewModelLazily {
		ApplicationSettingsViewModel(
			applicationSettingsRepository,
			selectedPlaybackEngineTypeAccess,
			libraryProvider,
			messageBus,
			syncScheduler,
		)
	}

	override val hiddenSettingsViewModel by activity.buildViewModelLazily {
		HiddenSettingsViewModel(applicationSettingsRepository)
	}

	override val userSslCertificateProvider by lazy {
		UserSslCertificateProvider(
			DocumentUriSelector(activitySuppliedDependencies.registeredActivityResultsLauncher),
			activity.contentResolver
		)
	}
	override val connectionStatusViewModel by activity.buildViewModelLazily {
		ConnectionStatusViewModel(
			stringResources,
			DramaticConnectionInitializationController(
				libraryConnectionProvider,
				applicationNavigation,
			),
		)
	}
}
