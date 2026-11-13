package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.promises.extensions.cancelBackEventually
import com.namehillsoftware.handoff.promises.Promise

class RemoteDataSourceFactoryProvider(private val connectionProvider: ProvideGuaranteedLibraryConnections) :
	ProvideRemoteDataSourceFactory
{
	override fun promiseRemoteDataSourceFactory(libraryId: LibraryId): Promise<HttpDataSource.Factory> =
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.cancelBackEventually { it.promiseDataSourceFactory() }
}
