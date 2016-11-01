package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

/**
 * Created by david on 11/1/16.
 */

public interface IProvidePlaybackQueues {
	IPreparedPlaybackFileProvider getQueue();
	IPreparedPlaybackFileProvider getRepeatingQueue();
	IPreparedPlaybackFileProvider getQueue(int startingAt);
	IPreparedPlaybackFileProvider getRepeatingQueue(int startingAt);
}
