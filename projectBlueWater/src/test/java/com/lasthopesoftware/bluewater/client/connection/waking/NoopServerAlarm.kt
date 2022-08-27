package com.lasthopesoftware.bluewater.client.connection.waking

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise

object NoopServerAlarm : WakeLibraryServer {
    override fun awakeLibraryServer(libraryId: LibraryId) = Unit.toPromise()
}
