package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.promises.IPromise;

import java.io.IOException;

/**
 * Created by david on 9/26/16.
 */

public interface IPreparedPlaybackFileProvider {
	IPromise<IPlaybackHandler> promisePreparedPlaybackFile(int pos) throws IOException;
}
