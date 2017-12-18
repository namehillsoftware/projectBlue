package com.lasthopesoftware.bluewater.client.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface IPlaybackPreparer {
	Promise<PreparedPlaybackFile> promisePreparedPlaybackFile(ServiceFile serviceFile, long preparedAt);
}
