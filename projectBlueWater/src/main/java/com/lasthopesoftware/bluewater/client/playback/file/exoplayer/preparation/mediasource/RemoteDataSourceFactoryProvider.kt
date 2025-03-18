package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import androidx.media3.datasource.DataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.lasthopesoftware.bluewater.client.connection.libraries.ProvideGuaranteedLibraryConnections
import com.lasthopesoftware.promises.extensions.cancelBackThen
import com.namehillsoftware.handoff.promises.Promise

class RemoteDataSourceFactoryProvider(private val connectionProvider: ProvideGuaranteedLibraryConnections) :
	ProvideRemoteDataSourceFactory
{
	override fun promiseRemoteDataSourceFactory(libraryId: LibraryId): Promise<DataSource.Factory> =
		connectionProvider
			.promiseLibraryConnection(libraryId)
			.cancelBackThen { it, _ -> it.dataSourceFactory }
}
