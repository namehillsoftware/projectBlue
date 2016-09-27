package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import java.io.Closeable;

public interface IMediaHandler extends Closeable {
	boolean isPlaying();
	void pause();
	void seekTo(int pos);
	void start();
	void stop();
}