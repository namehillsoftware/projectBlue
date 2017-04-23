package com.lasthopesoftware.bluewater.client.playback.state;

import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.state.volume.PlaylistVolumeManager;

import java.io.Closeable;
import java.io.IOException;

public final class PlaylistPlaybackIgniter implements IStartPlayback, Closeable {

	private final PlaylistVolumeManager volumeManagement;

	private PlaylistPlayer playlistPlayer;
	private ActivePlayer activePlayer;

	public PlaylistPlaybackIgniter(PlaylistVolumeManager volumeManagement) {
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
	public IActivePlayer startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException {
		close();

		playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, filePosition);
		activePlayer = new ActivePlayer(playlistPlayer, volumeManagement);

		return activePlayer;
	}

	@Override
	public void close() throws IOException {
		if (activePlayer != null) activePlayer.close();
		if (playlistPlayer != null)	playlistPlayer.close();
	}
}
