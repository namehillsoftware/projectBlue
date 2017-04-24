package com.lasthopesoftware.bluewater.client.playback.state;


import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.IPlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

public class ActivePlayer implements IActivePlayer, AutoCloseable {

	private final Disposable fileChangedObservableConnection;
	private final IPlaylistPlayer playlistPlayer;
	private final ConnectableObservable<PositionedPlaybackFile> observableProxy;

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
	public ConnectableObservable<PositionedPlaybackFile> observe() {
		return observableProxy;
	}

	@Override
	public void close() {
		fileChangedObservableConnection.dispose();
	}
}
