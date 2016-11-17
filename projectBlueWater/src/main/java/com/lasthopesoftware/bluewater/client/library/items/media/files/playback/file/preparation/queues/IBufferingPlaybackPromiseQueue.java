package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues;

import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 11/16/16.
 */

interface IBufferingPlaybackPromiseQueue {
	IPromise<PositionedBufferingPlaybackHandler> getNextPreparingMediaPlayerPromise(int preparedAt);
}
