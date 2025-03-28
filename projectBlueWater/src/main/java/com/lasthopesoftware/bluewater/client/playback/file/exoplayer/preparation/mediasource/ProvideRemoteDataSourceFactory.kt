package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import androidx.media3.datasource.DataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideRemoteDataSourceFactory {
    fun promiseRemoteDataSourceFactory(libraryId: LibraryId): Promise<DataSource.Factory>
}
