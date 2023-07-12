package com.lasthopesoftware.bluewater.client.playback.caching.datasource

import androidx.media3.datasource.DataSource
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource.ProvideHttpDataSourceFactory
import com.namehillsoftware.handoff.promises.Promise

class DiskFileCacheSourceFactory(
	private val dataSourceFactoryProvider: ProvideHttpDataSourceFactory,
	private val cacheStreamSupplier: SupplyCacheStreams
) {
	@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
	fun getDiskFileCacheSource(libraryId: LibraryId): Promise<DataSource.Factory> =
		dataSourceFactoryProvider.promiseHttpDataSourceFactory(libraryId).then { httpDataSourceFactory ->
			EntireFileCachedDataSource.Factory(libraryId, httpDataSourceFactory, cacheStreamSupplier)
		}
}
