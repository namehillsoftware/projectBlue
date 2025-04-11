package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory

import com.lasthopesoftware.bluewater.client.browsing.files.ServiceFile
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdatePlaystats
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.settings.LookupConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.MediaCenterConnectionSettings
import com.lasthopesoftware.bluewater.client.connection.settings.SubsonicConnectionSettings
import com.lasthopesoftware.bluewater.client.servers.version.ProvideLibraryServerVersion
import com.lasthopesoftware.policies.caching.PermanentPromiseFunctionCache
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.lasthopesoftware.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class LibraryPlaystatsUpdateSelector(
	private val connectionSettingsLookup: LookupConnectionSettings,
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
		connectionSettingsLookup
			.promiseConnectionSettings(libraryId)
			.cancelBackEventually {
				when (it) {
					is SubsonicConnectionSettings -> playedFilePlayStatsUpdater.toPromise()
					is MediaCenterConnectionSettings -> programVersionProvider
							.promiseServerVersion(libraryId)
							.cancelBackThen { v, _ ->
								if (v != null && v.major >= 22) playedFilePlayStatsUpdater
								else filePropertiesPlayStatsUpdater
							}
					else -> Promise(IllegalArgumentException("Unknown connection type"))
				}
			}
}
