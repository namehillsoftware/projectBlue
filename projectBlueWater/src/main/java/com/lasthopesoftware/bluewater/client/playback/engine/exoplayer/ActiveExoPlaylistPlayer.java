package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer;

import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;

import io.reactivex.observables.ConnectableObservable;

public class ActiveExoPlaylistPlayer implements IActivePlayer {
	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public ConnectableObservable<PositionedPlayingFile> observe() {
		return null;
	}
}
