package com.lasthopesoftware.bluewater.client.browsing

import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusableFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.list.ReusablePlaylistFileItemViewModelProvider
import com.lasthopesoftware.bluewater.client.browsing.files.properties.LibraryFilePropertiesDependents
import com.lasthopesoftware.bluewater.client.connection.libraries.LibraryConnectionDependents
import com.lasthopesoftware.bluewater.client.connection.session.ConnectionWatcherViewModel
import com.lasthopesoftware.bluewater.client.connection.session.initialization.ConnectionStatusViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.NowPlayingMessage
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingCoverArtViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingFilePropertiesViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.NowPlayingScreenViewModel
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.playlist.NowPlayingPlaylistViewModel
import com.lasthopesoftware.bluewater.shared.android.messages.ViewModelMessageBus

/**
 * View Models that work best when declared with a local ViewModelOwner
 */
interface ReusedViewModelDependencies : EntryDependencies, LibraryConnectionDependents, LibraryFilePropertiesDependents {
	val nowPlayingViewModelMessageBus: ViewModelMessageBus<NowPlayingMessage>
	val reusablePlaylistFileItemViewModelProvider: ReusablePlaylistFileItemViewModelProvider
	val reusableFileItemViewModelProvider: ReusableFileItemViewModelProvider
	val nowPlayingScreenViewModel: NowPlayingScreenViewModel
	val nowPlayingFilePropertiesViewModel: NowPlayingFilePropertiesViewModel
	val nowPlayingCoverArtViewModel: NowPlayingCoverArtViewModel
	val nowPlayingPlaylistViewModel: NowPlayingPlaylistViewModel
	val connectionStatusViewModel: ConnectionStatusViewModel
	val connectionWatcherViewModel: ConnectionWatcherViewModel
}