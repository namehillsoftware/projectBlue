package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file;

/**
 * Created by david on 9/19/16.
 */
public interface IMediaPlayerResourceManager {
	void initMediaPlayer();
	boolean isMediaPlayerCreated();
	void releaseMediaPlayer();
}
