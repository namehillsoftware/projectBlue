package com.lasthopesoftware.bluewater.client.playback.service.state;

import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackPlaylistStateManager;
import com.lasthopesoftware.promises.EmptyMessenger;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.TwoParameterFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

class PlayingPlaylistTrackChanger implements IStartedPlaylist, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaybackPlaylistStateManager.class);

	private final IPlaybackPreparerProvider playbackPreparerProvider;
	private final INowPlayingRepository nowPlayingRepository;
	private final IPositionedFileQueueProvider positionedFileQueueProvider;

	private PositionedPlaybackFile positionedPlaybackFile;
	private PlaylistPlayer playlistPlayer;
	private List<ServiceFile> playlist;
	private float volume;

	private PreparedPlaybackQueue preparedPlaybackQueue;
	private TwoParameterFunction<List<ServiceFile>, Integer, IPositionedFileQueue> positionedFileQueueGenerator;

	private Disposable fileChangedObservableConnection;
	private Disposable subscription;
	private ObservableEmitter<PositionedPlaybackFile> observableEmitter;

	PlayingPlaylistTrackChanger(IPlaybackPreparerProvider playbackPreparerProvider, INowPlayingRepository nowPlayingRepository, IPositionedFileQueueProvider positionedFileQueueProvider, CachedFilePropertiesProvider cachedFilePropertiesProvider) {
		this.playbackPreparerProvider = playbackPreparerProvider;
		this.nowPlayingRepository = nowPlayingRepository;
		this.positionedFileQueueProvider = positionedFileQueueProvider;
	}

	@Override
	public Promise<IPausedPlaylist> pause() {
		return null;
	}

	@Override
	public IStartedPlaylist playRepeatedly() {
		persistLibraryRepeating(true);

		updatePreparedFileQueueUsingState(positionedFileQueueProvider::getCyclicalQueue);

		return this;
	}

	@Override
	public IStartedPlaylist playToCompletion() {
		persistLibraryRepeating(false);

		updatePreparedFileQueueUsingState(positionedFileQueueProvider::getCompletableQueue);

		return this;
	}

	@Override
	public void setVolume(float volume) {
		this.volume = volume;

		if (playlistPlayer != null)
			playlistPlayer.setVolume(volume);
	}

	@Override
	public Promise<PositionedFile> changePosition(int playlistPosition, int filePosition) {
		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
			fileChangedObservableConnection.dispose();

		if (subscription != null)
			subscription.dispose();

		final Promise<NowPlaying> nowPlayingPromise =
			updateLibraryPlaylistPositions(playlistPosition, filePosition)
				.then(np -> {
					logger.info("Position changed");
					return np;
				});

		final Promise<PositionedFile> positionedFilePromise = new Promise<>(new EmptyMessenger<PositionedFile>() {
			@Override
			public void requestResolution() {
				final Promise<Observable<PositionedPlaybackFile>> observablePromise =
					nowPlayingPromise
						.then(PlayingPlaylistTrackChanger.this::initializePreparedPlaybackQueue)
						.then(q -> startPlayback(q, filePosition));

				observablePromise
					.then(observable -> observable.firstElement().subscribe(this::sendResolution))
					.error(runCarelessly(this::sendRejection));
			}
		});

		positionedFilePromise.error(runCarelessly(this::uncaughtExceptionHandler));

		return positionedFilePromise;
	}

	@Override
	public void subscribe(@NonNull ObservableEmitter<PositionedPlaybackFile> e) throws Exception {
		observableEmitter = e;
	}

	private void updatePreparedFileQueueUsingState(TwoParameterFunction<List<ServiceFile>, Integer, IPositionedFileQueue> newFileQueueGenerator) {
		positionedFileQueueGenerator = newFileQueueGenerator;

		updatePreparedFileQueueFromState();
	}

	private void updatePreparedFileQueueFromState() {
		if (preparedPlaybackQueue != null && playlist != null && positionedPlaybackFile != null)
			preparedPlaybackQueue.updateQueue(positionedFileQueueGenerator.resultFrom(playlist, positionedPlaybackFile.getPlaylistPosition() + 1));
	}

	private Promise<NowPlaying> updateLibraryPlaylistPositions(final int playlistPosition, final int filePosition) {
		final Promise<NowPlaying> nowPlayingPromise = nowPlayingRepository.getNowPlaying();

		return
			nowPlayingPromise
				.thenPromise(np -> {
					np.playlist = playlist;
					np.playlistPosition = playlistPosition;
					np.filePosition = filePosition;
					return nowPlayingRepository.updateNowPlaying(np);
				});
	}

	private Observable<PositionedPlaybackFile> startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException {
		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
			fileChangedObservableConnection.dispose();

		if (subscription != null)
			subscription.dispose();

		if (playlistPlayer != null)
			playlistPlayer.close();

		playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, filePosition);
		playlistPlayer.setVolume(volume);

		final ConnectableObservable<PositionedPlaybackFile> observableProxy = Observable.create(playlistPlayer).publish();

		subscription = observableProxy.subscribe(
			p -> {
				positionedPlaybackFile = p;

				if (observableEmitter != null)
					observableEmitter.onNext(p);

				saveStateToLibrary();
			},
			this::uncaughtExceptionHandler,
			() -> {
				saveStateToLibrary();
				changePosition(0, 0);
			});

		fileChangedObservableConnection = observableProxy.connect();

		return observableProxy;
	}

	private PreparedPlaybackQueue initializePreparedPlaybackQueue(NowPlaying nowPlaying) throws IOException {
		if (preparedPlaybackQueue != null)
			preparedPlaybackQueue.close();

		final int startPosition = Math.max(nowPlaying.playlistPosition, 0);

		if (positionedFileQueueGenerator == null) {
			positionedFileQueueGenerator =
				nowPlaying.isRepeating
					? positionedFileQueueProvider::getCyclicalQueue
					: positionedFileQueueProvider::getCompletableQueue;
		}

		return
			preparedPlaybackQueue =
				new PreparedPlaybackQueue(
					this.playbackPreparerProvider.providePlaybackPreparer(),
					positionedFileQueueGenerator.resultFrom(playlist, startPosition));
	}

	private Promise<NowPlaying> persistLibraryRepeating(boolean isRepeating) {
		return
			nowPlayingRepository
				.getNowPlaying()
				.then(result -> {
					result.isRepeating = isRepeating;

					nowPlayingRepository.updateNowPlaying(result);

					return result;
				});
	}

	private void saveStateToLibrary() {
		if (playlist == null) return;

		nowPlayingRepository
			.getNowPlaying()
			.thenPromise(np -> {
				np.playlist = playlist;

				if (positionedPlaybackFile != null) {
					np.playlistPosition = positionedPlaybackFile.getPlaylistPosition();
					np.filePosition = positionedPlaybackFile.getPlaybackHandler().getCurrentPosition();
				}

				return nowPlayingRepository.updateNowPlaying(np);
			});
	}

	private void uncaughtExceptionHandler(Throwable exception) {
		if (exception instanceof MediaPlayerException) {
			saveStateToLibrary();
			return;
		}

		logger.error("An uncaught error has occurred!", exception);
	}

	@Override
	public void close() throws IOException {
		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
			fileChangedObservableConnection.dispose();

		if (playlistPlayer != null)	playlistPlayer.close();
		if (preparedPlaybackQueue != null) preparedPlaybackQueue.close();
	}
}
