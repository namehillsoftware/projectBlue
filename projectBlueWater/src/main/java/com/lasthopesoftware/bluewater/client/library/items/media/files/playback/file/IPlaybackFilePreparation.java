package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.listeners.OnFilePreparedListener;

/**
 * Created by david on 9/17/16.
 */
public interface IPlaybackFilePreparation {
	boolean isPrepared();
	void prepareMediaPlayer();
	void prepareMpSynchronously();
	IPlaybackFilePreparation setOnFilePreparedListener(OnFilePreparedListener listener);
}
