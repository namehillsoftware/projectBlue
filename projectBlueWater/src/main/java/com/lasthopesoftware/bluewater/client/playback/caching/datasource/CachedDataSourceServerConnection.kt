package com.lasthopesoftware.bluewater.client.playback.caching.datasource

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.lasthopesoftware.bluewater.client.browsing.files.cached.stream.supplier.SupplyCacheStreams
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.live.LiveServerConnection
import com.lasthopesoftware.bluewater.client.connection.live.ProvideLiveServerConnection
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise


// ExoPlayer doesn't give us a good way to bundle a library ID in with a DataSpec request for the cache, so we will
// instead pass the library in through the constructor. This will make for some awkward object construction, but overhead
// should still be minimal.
class CachedDataSourceServerConnection(
	private val libraryId: LibraryId,
	private val inner: LiveServerConnection,
	private val cacheStreamSupplier: SupplyCacheStreams,
) : LiveServerConnection by inner {
	@OptIn(UnstableApi::class)
	override val dataSourceFactory: DataSource.Factory by lazy {
		EntireFileCachedDataSource.Factory(libraryId, inner.dataSourceFactory, cacheStreamSupplier)
	}
}

class CachedDataSourceServerConnectionProvider(
	private val liveServerConnections: ProvideLiveServerConnection,
	private val cacheStreamSupplier: SupplyCacheStreams
) : ProvideLiveServerConnection {
	override fun promiseLiveServerConnection(libraryId: LibraryId): Promise<LiveServerConnection?> =
		liveServerConnections
			.promiseLiveServerConnection(libraryId)
			.cancelBackThen { c, _ ->  c?.let { CachedDataSourceServerConnection(libraryId, it, cacheStreamSupplier) } }
}
