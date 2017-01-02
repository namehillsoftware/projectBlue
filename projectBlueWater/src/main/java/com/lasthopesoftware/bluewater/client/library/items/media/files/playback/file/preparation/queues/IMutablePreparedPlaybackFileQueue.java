package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

/**
 * Created by david on 9/26/16.
 */

public interface IMutablePreparedPlaybackFileQueue extends IPreparedPlaybackFileQueue {
	IMutablePreparedPlaybackFileQueue updateQueue(IBufferingPlaybackPromiseQueue newBufferingPlaybackPromiseQueue);
}
