package com.lasthopesoftware.bluewater

import com.lasthopesoftware.bluewater.android.intents.BuildIntents
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryProvider
import com.lasthopesoftware.bluewater.client.browsing.library.access.ILibraryStorage
import com.lasthopesoftware.bluewater.client.connection.session.ManageConnectionSessions
import com.lasthopesoftware.bluewater.client.stored.sync.SyncScheduler

interface ApplicationDependencies {
	val intentBuilder: BuildIntents
	val syncScheduler: SyncScheduler
	val sessionConnections: ManageConnectionSessions
	val libraryStorage: ILibraryStorage
	val libraryProvider: ILibraryProvider
}
