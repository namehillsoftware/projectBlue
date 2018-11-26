package com.lasthopesoftware.bluewater.client.playback.playlist;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayer extends ObservableOnSubscribe<PositionedPlayingFile> {
	void pause();
	void resume();

	void setVolume(float volume);

	boolean isPlaying();
}
