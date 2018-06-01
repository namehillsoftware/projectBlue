package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import com.google.android.exoplayer2.upstream.HttpDataSource;

public interface ProvideHttpDataSourceFactory {
	HttpDataSource.Factory getHttpDataSourceFactory();
}
