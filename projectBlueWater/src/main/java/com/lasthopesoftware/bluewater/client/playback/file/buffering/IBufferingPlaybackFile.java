package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import com.lasthopesoftware.messenger.promises.Promise;

public interface IBufferingPlaybackFile {
	Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile();
}
