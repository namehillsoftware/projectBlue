package com.lasthopesoftware.bluewater.client.playback.exoplayer

import androidx.media3.datasource.DataSource
import com.namehillsoftware.handoff.promises.Promise


interface ProvideServerHttpDataSource<TConnectionDetails> {
	fun promiseDataSourceFactory(connectionDetails: TConnectionDetails): Promise<DataSource.Factory>
}
