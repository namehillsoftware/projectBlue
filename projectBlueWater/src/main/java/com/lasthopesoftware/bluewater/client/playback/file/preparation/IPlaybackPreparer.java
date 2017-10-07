package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.messenger.promises.Promise;

/**
 * Created by david on 11/6/16.
 */

public interface IPlaybackPreparer {
	Promise<PreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, int preparedAt);
}
