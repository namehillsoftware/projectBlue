package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile

import com.lasthopesoftware.bluewater.client.access.ProvideRemoteLibraryAccess
import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.keepPromise
import com.namehillsoftware.handoff.promises.Promise

class PlayedFilePlayStatsUpdater(
	private val remoteLibraryAccess: ProvideRemoteLibraryAccess
) : UpdatePlayStatsWithPlayedSignal {
    override fun promisePlaystatsUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<*> =
		remoteLibraryAccess
			.promiseLibraryAccess(libraryId)
			.cancelBackEventually { it?.promisePlaystatsUpdate(serviceFile).keepPromise() }
}
