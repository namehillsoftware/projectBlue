package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayback extends IPromise<Void>, Closeable {
	void pause();
	void resume();

	void setVolume(float volume);
}
