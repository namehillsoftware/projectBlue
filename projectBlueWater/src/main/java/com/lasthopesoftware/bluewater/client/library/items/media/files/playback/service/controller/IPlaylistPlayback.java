package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.promises.IPromise;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayback extends IPromise<Void> {
	void pause();
	void resume();

	void setVolume(float volume);
}
