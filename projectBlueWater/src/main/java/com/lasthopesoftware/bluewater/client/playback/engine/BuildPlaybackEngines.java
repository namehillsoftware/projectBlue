package com.lasthopesoftware.bluewater.client.playback.engine;

import com.lasthopesoftware.bluewater.client.library.repository.Library;

public interface BuildPlaybackEngines {
	PlaybackEngine build(Library library);
}
