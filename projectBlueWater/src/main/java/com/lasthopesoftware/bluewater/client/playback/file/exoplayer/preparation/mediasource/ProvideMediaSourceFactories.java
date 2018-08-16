package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.net.Uri;

import com.google.android.exoplayer2.source.ExtractorMediaSource;

public interface ProvideMediaSourceFactories {
	ExtractorMediaSource.Factory getFactory(Uri uri);
}
