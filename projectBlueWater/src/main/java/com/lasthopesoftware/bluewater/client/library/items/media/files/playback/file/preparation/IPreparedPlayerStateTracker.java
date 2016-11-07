package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPreparedPlaybackFileProvider;

/**
 * Created by david on 11/1/16.
 */

public interface IPreparedPlayerStateTracker extends IPreparedPlaybackFileProvider {
	int getPreparedIndex();
}
