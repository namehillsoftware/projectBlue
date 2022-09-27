package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface UpdatePlaystats {
    fun promisePlaystatsUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<*>
}
