package com.lasthopesoftware.bluewater.client.playback.engine;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayableFile;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

public class ActivePlayer implements IActivePlayer, AutoCloseable {

	private final Disposable fileChangedObservableConnection;
	private final IPlaylistPlayer playlistPlayer;
	private final ConnectableObservable<PositionedPlayableFile> observableProxy;

	public ActivePlayer(IPlaylistPlayer playlistPlayer, PlaylistVolumeManager volumeManagement) {
		this.playlistPlayer = playlistPlayer;

		volumeManagement.managePlayer(playlistPlayer);

		observableProxy = Observable.create(playlistPlayer).replay(1);

		fileChangedObservableConnection = observableProxy.connect();
	}

	@Override
	public void pause() {
		playlistPlayer.pause();
	}

	@Override
	public void resume() {
		playlistPlayer.resume();
	}

	@Override
	public ConnectableObservable<PositionedPlayableFile> observe() {
		return observableProxy;
	}

	@Override
	public void close() {
		fileChangedObservableConnection.dispose();
	}
}
