package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.IPlaybackFileErrorBroadcaster;

import java.io.IOException;

/**
 * Created by david on 9/17/16.
 */
public interface IPlaybackFilePreparation<TErrorData> extends IPlaybackFileErrorBroadcaster<TErrorData> {
	boolean isPrepared();
	void prepareMediaPlayer();
	void prepareMpSynchronously();
	IPlaybackFilePreparation setOnFilePreparedListener(OnFilePreparedListener listener);
	boolean isBuffered();
	int getBufferPercentage();
	void setOnFileBufferedListener(OnFileBufferedListener listener);
	IPlaybackFile getPlaybackFile();
}
