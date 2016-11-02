package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

/**
 * Created by david on 11/1/16.
 */

public interface IProvidePlaybackQueues {
	IPreparedPlaybackFileProvider getQueue(boolean isCyclical);
	IPreparedPlaybackFileProvider getQueue(boolean isCyclical, int startingAt);
}
