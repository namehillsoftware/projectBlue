package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import androidx.media3.datasource.HttpDataSource
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId
import com.namehillsoftware.handoff.promises.Promise

interface ProvideHttpDataSourceFactory {
    fun promiseHttpDataSourceFactory(libraryId: LibraryId): Promise<HttpDataSource.Factory>
}
