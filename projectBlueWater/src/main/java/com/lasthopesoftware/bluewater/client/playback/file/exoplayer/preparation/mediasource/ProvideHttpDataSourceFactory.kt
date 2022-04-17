package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource

import com.google.android.exoplayer2.upstream.HttpDataSource

interface ProvideHttpDataSourceFactory {
    fun getHttpDataSourceFactory(): HttpDataSource.Factory
}
