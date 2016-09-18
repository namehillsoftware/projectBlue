package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFileErrorListener;

public interface IPlaybackFile extends IPlaybackFilePreparation {
	IFile getFile();
	void initMediaPlayer();
	boolean isMediaPlayerCreated();
	void releaseMediaPlayer();
	int getCurrentPosition();
	boolean isBuffered();
	int getBufferPercentage();
	int getDuration();
	boolean isPlaying();
	void pause();
	void seekTo(int pos);
	void start();
	void stop();
	float getVolume();
	void setVolume(float volume);
	
	/* Listener methods */
	void addOnFileCompleteListener(OnFileCompleteListener listener);
	void removeOnFileCompleteListener(OnFileCompleteListener listener);
	void addOnFileErrorListener(OnFileErrorListener listener);
	void removeOnFileErrorListener(OnFileErrorListener listener);
	void addOnFileBufferedListener(OnFileBufferedListener listener);
	void removeOnFileErrorListener(OnFileBufferedListener listener);
}
