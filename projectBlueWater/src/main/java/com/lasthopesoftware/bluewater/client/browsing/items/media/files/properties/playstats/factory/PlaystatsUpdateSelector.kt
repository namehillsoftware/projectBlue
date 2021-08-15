package com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.factory

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.ProvideScopedFileProperties
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.IPlaystatsUpdate
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.fileproperties.FilePropertiesPlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.playstats.playedfile.PlayedFilePlayStatsUpdater
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.properties.storage.ScopedFilePropertiesStorage
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
	private var promisedPlaystatsUpdater = Promise.empty<IPlaystatsUpdate>()

	fun promisePlaystatsUpdater(): Promise<IPlaystatsUpdate> =
		synchronized(sync) {
			promisedPlaystatsUpdater.eventually(
				{ u -> u?.toPromise() ?: promiseNewPlaystatsUpdater() },
				{ promiseNewPlaystatsUpdater() })
		}

	private fun promiseNewPlaystatsUpdater(): Promise<IPlaystatsUpdate> =
		synchronized(sync) {
			programVersionProvider.promiseServerVersion()
				.then { v ->
					if (v != null && v.major >= 22) PlayedFilePlayStatsUpdater(connectionProvider)
					else FilePropertiesPlayStatsUpdater(filePropertiesProvider, scopedFilePropertiesStorage)
				}
				.also {
					promisedPlaystatsUpdater = it
				}
		}
}
