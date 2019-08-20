package com.lasthopesoftware.bluewater.client.playback.file.exoplayer.preparation.mediasource;

import android.net.Uri;

import com.google.android.exoplayer2.source.MediaSource;

public interface SpawnMediaSources {
	MediaSource getNewMediaSource(Uri uri);
}
