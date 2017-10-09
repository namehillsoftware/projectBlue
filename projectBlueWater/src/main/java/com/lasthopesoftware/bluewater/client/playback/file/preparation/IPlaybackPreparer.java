package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.messenger.promises.Promise;

public interface IPlaybackPreparer {
	Promise<PreparedPlaybackFile> promisePreparedPlaybackHandler(ServiceFile serviceFile, long preparedAt);
}
