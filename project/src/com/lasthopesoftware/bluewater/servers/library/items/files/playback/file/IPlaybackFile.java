package com.lasthopesoftware.bluewater.servers.library.items.files.playback.file;

import java.io.IOException;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFilePreparedListener;

public interface IPlaybackFile {
	File getFile();
	void initMediaPlayer();
	boolean isMediaPlayerCreated();
	boolean isPrepared();
	void prepareMediaPlayer();
	void prepareMpSynchronously();
	void releaseMediaPlayer();
	int getCurrentPosition();
	boolean isBuffered();
	int getBufferPercentage();
	int getDuration() throws IOException;
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
	void addOnFilePreparedListener(OnFilePreparedListener listener);
	void removeOnFilePreparedListener(OnFilePreparedListener listener);
	void addOnFileErrorListener(OnFileErrorListener listener);
	void removeOnFileErrorListener(OnFileErrorListener listener);
	void addOnFileBufferedListener(OnFileBufferedListener listener);
	void removeOnFileErrorListener(OnFileBufferedListener listener);
}
