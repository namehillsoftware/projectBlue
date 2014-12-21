package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.io.IOException;

import com.lasthopesoftware.bluewater.data.service.objects.File;

public interface IFilePlayer {
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
}
