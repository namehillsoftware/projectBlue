package com.lasthopesoftware.bluewater.client.playback.file.specs.fakes;

import com.lasthopesoftware.bluewater.client.playback.file.PlayableFile;
import com.lasthopesoftware.bluewater.client.playback.file.buffering.IBufferingPlaybackFile;
import com.namehillsoftware.handoff.promises.Promise;

public class FakeBufferingPlaybackHandler
implements
	IBufferingPlaybackFile,
	PlayableFile
{
	private boolean isPlaying;
	private int currentPosition;

	@Override
	public boolean isPlaying() {
		return isPlaying;
	}

	@Override
	public void pause() {
		isPlaying = false;
	}

	public void setCurrentPosition(int position) {
		this.currentPosition = position;
	}

	@Override
	public long getDuration() {
		return 0;
	}

	@Override
	public Promise<PlayableFile> promisePlayback() {
		isPlaying = true;
		return new Promise<>((messenger) -> {});
	}

	@Override
	public void close() {

	}

	@Override
	public Promise<IBufferingPlaybackFile> promiseBufferedPlaybackFile() {
		return new Promise<>(this);
	}
}
