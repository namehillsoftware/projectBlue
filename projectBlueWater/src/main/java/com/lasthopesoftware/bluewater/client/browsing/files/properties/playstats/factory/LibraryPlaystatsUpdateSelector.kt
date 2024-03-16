package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlaystats
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.servers.version.ProvideLibraryServerVersion
import com.lasthopesoftware.policies.caching.PermanentPromiseFunctionCache
import com.namehillsoftware.handoff.promises.Promise

class LibraryPlaystatsUpdateSelector(
	private val programVersionProvider: ProvideLibraryServerVersion,
	private val playedFilePlayStatsUpdater: PlayedFilePlayStatsUpdater,
	private val filePropertiesPlayStatsUpdater: FilePropertiesPlayStatsUpdater
) : UpdatePlaystats {
	private val promiseCache = PermanentPromiseFunctionCache<LibraryId, UpdatePlaystats>()

	override fun promisePlaystatsUpdate(libraryId: LibraryId, serviceFile: ServiceFile): Promise<*> =
		promisePlaystatsUpdater(libraryId)
			.eventually { it.promisePlaystatsUpdate(libraryId, serviceFile) }

	private fun promisePlaystatsUpdater(libraryId: LibraryId): Promise<UpdatePlaystats> =
		promiseCache.getOrAdd(libraryId, ::promiseNewPlaystatsUpdater)

	private fun promiseNewPlaystatsUpdater(libraryId: LibraryId): Promise<UpdatePlaystats> =
		programVersionProvider
			.promiseServerVersion(libraryId)
			.then { v ->
				if (v != null && v.major >= 22) playedFilePlayStatsUpdater
				else filePropertiesPlayStatsUpdater
			}
}
