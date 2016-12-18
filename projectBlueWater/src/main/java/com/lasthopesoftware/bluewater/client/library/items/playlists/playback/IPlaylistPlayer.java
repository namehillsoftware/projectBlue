package com.lasthopesoftware.bluewater.client.library.items.playlists.playback;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;

import java.io.Closeable;

import io.reactivex.ObservableSource;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayer extends ObservableSource<PositionedPlaybackFile>, Closeable {
	void pause();
	void resume();

	void setVolume(float volume);

	void cancel();
}
