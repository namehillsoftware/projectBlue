package com.lasthopesoftware.bluewater.client.playback.engine.exoplayer;

import com.google.android.exoplayer2.ExoPlayer;
import com.lasthopesoftware.bluewater.client.playback.engine.IActivePlayer;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;

import io.reactivex.observables.ConnectableObservable;

public class ActiveExoPlaylistPlayer implements IActivePlayer {
	private final ExoPlayer exoPlayer;

	public ActiveExoPlaylistPlayer(ExoPlayer exoPlayer) {
		this.exoPlayer = exoPlayer;
		exoPlayer.setPlayWhenReady(true);
	}

	@Override
	public void pause() {
		this.exoPlayer.setPlayWhenReady(false);
	}

	@Override
	public void resume() {
		this.exoPlayer.setPlayWhenReady(true);
	}

	@Override
	public ConnectableObservable<PositionedPlayingFile> observe() {
		return null;
	}
}
