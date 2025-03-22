package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideLibraryConnections
import com.lasthopesoftware.bluewater.client.connection.live.eventuallyFromDataAccess
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class PlayedFilePlayStatsUpdater(
	private val libraryConnections: ProvideLibraryConnections
) : UpdatePlayStatsWithPlayedSignal {
    override fun promisePlaystatsUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<*> =
		libraryConnections
			.promiseLibraryConnection(libraryId)
			.eventuallyFromDataAccess { it?.promisePlaystatsUpdate(serviceFile).keepPromise() }
}
