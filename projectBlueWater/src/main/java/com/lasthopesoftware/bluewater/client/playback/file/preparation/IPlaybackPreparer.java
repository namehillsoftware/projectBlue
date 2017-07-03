package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackHandler;
import com.lasthopesoftware.messenger.promise.Promise;

/**
 * Created by david on 11/6/16.
 */

public interface IPlaybackPreparer {
	Promise<IBufferingPlaybackHandler> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt);
}
