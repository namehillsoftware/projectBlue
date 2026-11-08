package com.lasthopesoftware.bluewater.client.playback.exoplayer

import androidx.media3.datasource.HttpDataSource
import com.namehillsoftware.handoff.promises.Promise


interface ProvideServerHttpDataSource<TConnectionDetails> {
	fun promiseDataSourceFactory(connectionDetails: TConnectionDetails): Promise<HttpDataSource.Factory>
}
