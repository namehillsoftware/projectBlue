package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.browsing.files.cached.DiskFileCache
import com.lasthopesoftware.bluewater.client.browsing.files.cached.persistence.UpdateDiskFileAccessTime
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.playback.service.ControlPlaybackService
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler
import com.lasthopesoftware.bluewater.shared.android.ui.ProvideScreenDimensions
import com.lasthopesoftware.bluewater.shared.images.ProvideDefaultImage
import com.lasthopesoftware.bluewater.shared.messages.application.RegisterForApplicationMessages
import com.lasthopesoftware.bluewater.shared.messages.application.SendApplicationMessages

interface ApplicationDependencies {
	val connectionSessions: ManageConnectionSessions
	val libraryConnectionProvider: ProvideLibraryConnections
	val sendApplicationMessages: SendApplicationMessages
	val registerForApplicationMessages: RegisterForApplicationMessages
	val intentBuilder: BuildIntents
	val syncScheduler: SyncScheduler
	val defaultImageProvider: ProvideDefaultImage
	val diskFileAccessTimeUpdater: UpdateDiskFileAccessTime
	val playbackServiceController: ControlPlaybackService
	val imageDiskFileCache: DiskFileCache
	val screenDimensions: ProvideScreenDimensions
}

