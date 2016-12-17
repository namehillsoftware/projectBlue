package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.promises.IPromise;

import java.io.Closeable;
import java.util.Collection;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayer extends IPromise<Collection<PositionedPlaybackFile>>, Closeable {
	void pause();
	void resume();

	void setVolume(float volume);
}
