package com.lasthopesoftware.bluewater.client.playback.engine;


import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlayingFile;
import com.lasthopesoftware.bluewater.client.playback.playlist.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.volume.PlaylistVolumeManager;
import com.namehillsoftware.handoff.promises.Promise;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

public class ActivePlayer implements IActivePlayer, AutoCloseable {

	private final Disposable fileChangedObservableConnection;
	private final IPlaylistPlayer playlistPlayer;
	private final ConnectableObservable<PositionedPlayingFile> observableProxy;

	public ActivePlayer(IPlaylistPlayer playlistPlayer, PlaylistVolumeManager volumeManagement) {
		this.playlistPlayer = playlistPlayer;

		volumeManagement.managePlayer(playlistPlayer);

		observableProxy = Observable.create(playlistPlayer).replay(1);

		fileChangedObservableConnection = observableProxy.connect();
	}

	@Override
	public Promise<?> pause() {
		return playlistPlayer.pause();
	}

	@Override
	public Promise<?> resume() {
		return playlistPlayer.resume();
	}

	@Override
	public ConnectableObservable<PositionedPlayingFile> observe() {
		return observableProxy;
	}

	@Override
	public void close() {
		fileChangedObservableConnection.dispose();
	}
}
