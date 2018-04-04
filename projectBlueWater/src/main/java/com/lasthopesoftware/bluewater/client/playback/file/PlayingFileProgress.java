package com.lasthopesoftware.bluewater.client.playback.file;

public class PlayingFileProgress {
	public final long position;
	public final long duration;

	public PlayingFileProgress(long position, long duration) {
		this.position = position;
		this.duration = duration;
	}
}
