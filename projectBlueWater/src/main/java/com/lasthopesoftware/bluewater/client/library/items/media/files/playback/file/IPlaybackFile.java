package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileErrorListener;

public interface IPlaybackFile extends IPlaybackFilePreparation, IPlaybackFileController {
	IFile getFile();
	void initMediaPlayer();
	boolean isMediaPlayerCreated();
	void releaseMediaPlayer();
	int getCurrentPosition();
	boolean isBuffered();
	int getBufferPercentage();
	int getDuration();
	float getVolume();
	void setVolume(float volume);
	
	/* Listener methods */
	void addOnFileErrorListener(OnFileErrorListener listener);
	void removeOnFileErrorListener(OnFileErrorListener listener);
	void addOnFileBufferedListener(OnFileBufferedListener listener);
	void removeOnFileErrorListener(OnFileBufferedListener listener);
}
