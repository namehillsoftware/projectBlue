package com.lasthopesoftware.bluewater.client.playback.engine;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;

import io.reactivex.observables.ConnectableObservable;

public interface IActivePlayer {
	void pause();
	void resume();
	ConnectableObservable<PositionedPlayingFile> observe();
}
