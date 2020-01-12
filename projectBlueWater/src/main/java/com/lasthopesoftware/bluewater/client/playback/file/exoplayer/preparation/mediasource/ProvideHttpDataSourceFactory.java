package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;

public interface ProvideHttpDataSourceFactory {
	HttpDataSource.Factory getHttpDataSourceFactory(Library library);
}
