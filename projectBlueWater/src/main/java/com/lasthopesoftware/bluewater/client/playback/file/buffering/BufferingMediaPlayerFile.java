package com.lasthopesoftware.bluewater.client.playback.file.buffering;

import android.media.MediaPlayer;

import com.lasthopesoftware.messenger.promises.Promise;

public class BufferingMediaPlayerFile
implements IBufferingPlaybackFile {

	private final Promise<IBufferingPlaybackFile> bufferingPromise;

	public BufferingMediaPlayerFile(MediaPlayer mediaPlayer) {
		bufferingPromise = new Promise<>(new BufferingMediaPlayerTask(this, mediaPlayer));
	}

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return bufferingPromise;
	}
}
