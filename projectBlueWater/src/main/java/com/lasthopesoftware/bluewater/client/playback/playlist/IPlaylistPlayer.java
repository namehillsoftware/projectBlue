package com.lasthopesoftware.bluewater.client.playback.playlist;

import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.namehillsoftware.handoff.promises.Promise;

import io.reactivex.ObservableOnSubscribe;

/**
 * Created by david on 11/7/16.
 */

public interface IPlaylistPlayer extends ObservableOnSubscribe<PositionedPlayingFile> {
	Promise<Void> pause();
	Promise<Void> resume();

	void setVolume(float volume);

	boolean isPlaying();
}
