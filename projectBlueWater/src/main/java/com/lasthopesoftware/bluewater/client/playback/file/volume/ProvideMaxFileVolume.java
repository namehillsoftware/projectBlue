package com.lasthopesoftware.bluewater.client.playback.file.volume;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.namehillsoftware.handoff.promises.Promise;

public interface ProvideMaxFileVolume {
	Promise<Float> promiseMaxFileVolume(ServiceFile serviceFile);
}
