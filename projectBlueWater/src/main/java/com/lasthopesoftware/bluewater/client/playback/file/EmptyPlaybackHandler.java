package com.lasthopesoftware.bluewater.client.playback.file;

import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;

import java.io.IOException;

public class EmptyPlaybackHandler
implements IBufferingPlaybackFile, PlayableFile {

	private final int duration;

	public EmptyPlaybackHandler(int duration) {
		this.duration = duration;
	}

	@Override
	public boolean isPlaying() {
		return false;
	}

	@Override
	public void pause() {

	}

	@Override
	public long getCurrentPosition() {
		return 0;
	}

	@Override
	public long getDuration() {
		return duration;
	}

	@Override
	public Promise<PlayableFile> promisePlayback() {
		return new Promise<>(this);
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return new Promise<>(this);
	}
}
