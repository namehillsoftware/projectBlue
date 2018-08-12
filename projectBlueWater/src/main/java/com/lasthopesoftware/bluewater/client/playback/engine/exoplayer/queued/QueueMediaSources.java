package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer.queued;

import com.google.android.exoplayer2.source.MediaSource;
import com.namehillsoftware.handoff.promises.Promise;

public interface QueueMediaSources {
	Promise<MediaSource> enqueueMediaSource(MediaSource mediaSource);
}
