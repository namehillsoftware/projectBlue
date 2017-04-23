package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.state.volume.IVolumeManagement;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;

import java.io.Closeable;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

public final class ActivePlaylist implements IStartPlayback, Closeable {

	private final PlaylistVolumeManager volumeManagement;

	private PlaylistPlayer playlistPlayer;
	private Disposable fileChangedObservableConnection;

	public ActivePlaylist(PlaylistVolumeManager volumeManagement) {
		this.volumeManagement = volumeManagement;
	}

//	@Override
//	public Promise<PositionedFile> changePosition(int playlistPosition, int filePosition) {
//		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
//			fileChangedObservableConnection.dispose();
//
//		if (subscription != null)
//			subscription.dispose();
//
//		final Promise<NowPlaying> nowPlayingPromise =
//			updateLibraryPlaylistPositions(playlistPosition, filePosition)
//				.then(np -> {
//					logger.info("Position changed");
//					return np;
//				});
//
//		final Promise<PositionedFile> positionedFilePromise = new Promise<>(new EmptyMessenger<PositionedFile>() {
//			@Override
//			public final void requestResolution() {
//				final Promise<Observable<PositionedPlaybackFile>> observablePromise =
//					nowPlayingPromise
//						.then(np -> ActivePlaylist.this.getNewPositionedFileQueue(playlistPosition))
//						.then(preparedPlaybackQueueResourceManagement::initializePreparedPlaybackQueue)
//						.then(q -> startPlayback(q, filePosition));
//
//				observablePromise
//					.then(observable -> observable.firstElement().subscribe(this::sendResolution))
//					.error(runCarelessly(this::sendRejection));
//			}
//		});
//
//		positionedFilePromise.error(runCarelessly(this::uncaughtExceptionHandler));
//
//		return positionedFilePromise;
//	}

	@Override
	public ConnectableObservable<PositionedPlaybackFile> startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException {
		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
			fileChangedObservableConnection.dispose();

		if (playlistPlayer != null)
			playlistPlayer.close();

		playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, filePosition);
		volumeManagement.managePlayer(playlistPlayer);

		final ConnectableObservable<PositionedPlaybackFile> observableProxy = Observable.create(playlistPlayer).replay(1);

		fileChangedObservableConnection = observableProxy.connect();

		return observableProxy;
	}

	@Override
	public IVolumeManagement manageVolume() {
		return volumeManagement;
	}

	@Override
	public void close() throws IOException {
		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
			fileChangedObservableConnection.dispose();

		if (playlistPlayer != null)	playlistPlayer.close();
	}
}
