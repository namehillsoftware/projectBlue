package com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.factory

import com.lasthopesoftware.bluewater.client.browsing.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.UpdateScopedPlaystats
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.fileproperties.ScopedFilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.playstats.playedfile.ScopedPlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.files.properties.storage.ScopedFilePropertiesStorage
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider
import com.lasthopesoftware.bluewater.client.servers.version.IProgramVersionProvider
import com.lasthopesoftware.bluewater.shared.promises.extensions.toPromise
import com.namehillsoftware.handoff.promises.Promise

class PlaystatsUpdateSelector(
	private val connectionProvider: IConnectionProvider,
	private val filePropertiesProvider: ProvideScopedFileProperties,
	private val scopedFilePropertiesStorage: ScopedFilePropertiesStorage,
	private val programVersionProvider: IProgramVersionProvider
) {
	private val sync = Any()

	@Volatile
	private var promisedPlaystatsUpdater = Promise.empty<UpdateScopedPlaystats>()

	fun promisePlaystatsUpdater(): Promise<UpdateScopedPlaystats> =
		synchronized(sync) {
			promisedPlaystatsUpdater.eventually(
				{ u -> u?.toPromise() ?: promiseNewPlaystatsUpdater() },
				{ promiseNewPlaystatsUpdater() })
		}

	private fun promiseNewPlaystatsUpdater(): Promise<UpdateScopedPlaystats> =
		synchronized(sync) {
			programVersionProvider.promiseServerVersion()
				.then { v ->
					if (v != null && v.major >= 22) ScopedPlayedFilePlayStatsUpdater(connectionProvider)
					else ScopedFilePropertiesPlayStatsUpdater(filePropertiesProvider, scopedFilePropertiesStorage)
				}
				.also {
					promisedPlaystatsUpdater = it
				}
		}
}
