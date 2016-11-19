package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;
import java.util.Collection;

import rx.Observable;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayback extends IPromise<Collection<PositionedPlaybackFile>>, Closeable {
	void pause();
	void resume();

	void setVolume(float volume);

	Observable<PositionedPlaybackFile> observePlaybackChanges();
}
