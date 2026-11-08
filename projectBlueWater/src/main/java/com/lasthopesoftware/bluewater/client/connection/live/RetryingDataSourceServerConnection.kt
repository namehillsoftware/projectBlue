package com.lasthopesoftware.bluewater.client.connection.live

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.playback.exoplayer.RetryingExoPlayerDataSource
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class RetryingDataSourceServerConnection(private val inner: LiveServerConnection) : LiveServerConnection by inner {
	override fun promiseDataSourceFactory(): Promise<HttpDataSource.Factory> =
		inner.promiseDataSourceFactory()
			.cancelBackThen { factory, _ ->
				RetryingExoPlayerDataSource.Factory(factory)
			}
}

class RetryingDataSourceServerConnectionProvider(private val liveServerConnections: ProvideLiveServerConnection) : ProvideLiveServerConnection {
	@OptIn(UnstableApi::class)
	override fun promiseLiveServerConnection(libraryId: LibraryId): Promise<LiveServerConnection?> =
		liveServerConnections
			.promiseLiveServerConnection(libraryId)
			.cancelBackThen { c, _ ->  c?.let { RetryingDataSourceServerConnection(it) } }
}
