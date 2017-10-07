package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import com.lasthopesoftware.messenger.promises.Promise;

public class BufferingExoPlayer implements IBufferingPlaybackFile {
	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return null;
	}
}
