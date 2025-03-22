package com.lasthopesoftware.bluewater.client.connection.live

import com.lasthopesoftware.bluewater.client.access.RemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.namehillsoftware.handoff.promises.Promise

interface ProvideLiveServerConnection {
	fun promiseLiveServerConnection(libraryId: LibraryId): Promise<LiveServerConnection?>
}

inline fun <Resolution> Promise<LiveServerConnection?>.eventuallyFromDataAccess(crossinline factory: (RemoteLibraryAccess?) -> Promise<Resolution>): Promise<Resolution> =
	cancelBackEventually { it?.dataAccess.let(factory) }
