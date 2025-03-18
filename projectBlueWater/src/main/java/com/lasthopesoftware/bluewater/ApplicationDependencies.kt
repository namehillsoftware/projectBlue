package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.UpdateDiskFileAccessTime
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.browsing.library.access.session.ProvideSelectedLibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.playback.nowplaying.storage.GetNowPlayingState
import com.lasthopesoftware.bluewater.client.playback.nowplaying.view.viewmodels.StoreNowPlayingDisplaySettings
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.library.items.AccessStoredItems
import com.lasthopesoftware.bluewater.client.stored.sync.ScheduleSyncs
import com.lasthopesoftware.bluewater.settings.repository.access.HoldApplicationSettings
import com.lasthopesoftware.bluewater.shared.android.ui.ProvideScreenDimensions
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages
import com.lasthopesoftware.resources.bitmaps.ProduceBitmaps
import com.lasthopesoftware.resources.strings.GetStringResources

interface ApplicationDependencies {
	val bitmapProducer: ProduceBitmaps
	val libraryProvider: ILibraryProvider
	val libraryStorage: ILibraryStorage
	val storedItemAccess: AccessStoredItems
	val connectionSessions: ManageConnectionSessions
	val libraryConnectionProvider: ProvideLibraryConnections
	val sendApplicationMessages: SendApplicationMessages
	val registerForApplicationMessages: RegisterForApplicationMessages
	val intentBuilder: BuildIntents
	val syncScheduler: ScheduleSyncs
	val defaultImageProvider: ProvideDefaultImage
	val diskFileAccessTimeUpdater: UpdateDiskFileAccessTime
	val playbackServiceController: ControlPlaybackService
	val imageDiskFileCache: DiskFileCache
	val screenDimensions: ProvideScreenDimensions
	val selectedLibraryIdProvider: ProvideSelectedLibraryId
	val stringResources: GetStringResources
	val applicationSettings: HoldApplicationSettings
	val nowPlayingState: GetNowPlayingState
	val nowPlayingDisplaySettings: StoreNowPlayingDisplaySettings
	val audioFileCache: DiskFileCache
}

