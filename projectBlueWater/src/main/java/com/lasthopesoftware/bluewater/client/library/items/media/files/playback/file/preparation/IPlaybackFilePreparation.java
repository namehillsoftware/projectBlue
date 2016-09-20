package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

/**
 * Created by david on 9/17/16.
 */
public interface IPlaybackFilePreparation {
	boolean isPrepared();
	void prepareMediaPlayer();
	void prepareMpSynchronously();
	IPlaybackFilePreparation setOnFilePreparedListener(OnFilePreparedListener listener);
	boolean isBuffered();
	int getBufferPercentage();
	void setOnFileBufferedListener(OnFileBufferedListener listener);
}
