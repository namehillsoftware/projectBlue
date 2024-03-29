package com.lasthopesoftware.bluewater.client.browsing

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.lasthopesoftware.bluewater.NavigateApplication
import com.lasthopesoftware.bluewater.client.browsing.files.access.LibraryFileProvider
import com.lasthopesoftware.bluewater.client.browsing.files.access.ProvideItemFiles
import com.lasthopesoftware.bluewater.client.browsing.files.list.FileListViewModel
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.SearchFilesViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.access.ProvideItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ItemListViewModel
import com.lasthopesoftware.bluewater.client.browsing.items.list.PlaybackLibraryItems
import com.lasthopesoftware.bluewater.client.browsing.items.list.ReusableChildItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers.ItemListMenuBackPressedHandler
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.RemoveLibraries
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.SelectedLibraryViewModel
import com.lasthopesoftware.bluewater.client.browsing.navigation.NavigationMessage
import com.lasthopesoftware.bluewater.client.connection.polling.PollForLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.connection.trust.ProvideUserSslCertificates
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackServiceController
import com.lasthopesoftware.bluewater.client.settings.LibrarySettingsViewModel
import com.lasthopesoftware.bluewater.client.stored.library.items.StateChangeBroadcastingStoredItemAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.StoredFileAccess
import com.lasthopesoftware.bluewater.client.stored.library.items.files.view.ActiveFileDownloadsViewModel
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.settings.ApplicationSettingsViewModel
import com.lasthopesoftware.bluewater.settings.hidden.HiddenSettingsViewModel
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.messages.RegisterForTypedMessages
import com.lasthopesoftware.bluewater.shared.messages.application.ScopedApplicationMessageBus
import com.lasthopesoftware.resources.strings.StringResources

@OptIn(UnstableApi::class) interface BrowserViewDependencies {
	val selectedLibraryViewModel: SelectedLibraryViewModel
	val nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel
	val itemProvider: ProvideItems
	val messageBus: ScopedApplicationMessageBus
	val storedItemAccess: StateChangeBroadcastingStoredItemAccess
	val playbackServiceController: PlaybackServiceController
	val itemFileProvider: ProvideItemFiles
	val itemListMenuBackPressedHandler: ItemListMenuBackPressedHandler
	val stringResources: StringResources
	val libraryFilesProvider: LibraryFileProvider
	val applicationNavigation: NavigateApplication
	val libraryConnectionProvider: ManageConnectionSessions
	val storedFileAccess: StoredFileAccess
	val syncScheduler: SyncScheduler
	val libraryProvider: ILibraryProvider
	val libraryStorage: ILibraryStorage
	val libraryRemoval: RemoveLibraries
	val navigationMessages: RegisterForTypedMessages<NavigationMessage>
	val applicationSettingsRepository: HoldApplicationSettings
	val playbackLibraryItems: PlaybackLibraryItems
	val nowPlayingState: GetNowPlayingState
	val pollForConnections: PollForLibraryConnections
	val nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel
	val nowPlayingPlaylistViewModel: NowPlayingPlaylistViewModel
	val connectionWatcherViewModel: ConnectionWatcherViewModel
	val reusablePlaylistFileItemViewModelProvider: ReusablePlaylistFileItemViewModelProvider
	val reusableChildItemViewModelProvider: ReusableChildItemViewModelProvider
	val reusableFileItemViewModelProvider: ReusableFileItemViewModelProvider
	val applicationSettingsViewModel: ApplicationSettingsViewModel
	val hiddenSettingsViewModel: HiddenSettingsViewModel
	val userSslCertificateProvider: ProvideUserSslCertificates
}

/**
 * View Models that work best when declared with a local ViewModelOwner
 */
interface ScopedBrowserViewDependencies : BrowserViewDependencies {
	val itemListViewModel: ItemListViewModel
	val fileListViewModel: FileListViewModel
	val activeFileDownloadsViewModel: ActiveFileDownloadsViewModel
	val searchFilesViewModel: SearchFilesViewModel
	val librarySettingsViewModel: LibrarySettingsViewModel
}
