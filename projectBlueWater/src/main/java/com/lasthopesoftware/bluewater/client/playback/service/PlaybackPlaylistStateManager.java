package com.lasthopesoftware.bluewater.client.playback.service;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public class PlaybackPlaylistStateManager implements ObservableOnSubscribe<PositionedPlaybackFile>, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaybackPlaylistStateManager.class);

	private final IPlaybackPreparerProvider playbackPreparerProvider;
	private final INowPlayingRepository nowPlayingRepository;
	private final Map<Boolean, IPositionedFileQueueProvider> positionedFileQueueProviders;
	private final CachedFilePropertiesProvider cachedFilePropertiesProvider;

	private PositionedPlaybackFile positionedPlaybackFile;
	private PlaylistPlayer playlistPlayer;
	private List<ServiceFile> playlist;
	private float volume;
	private boolean isPlaying;

	private PreparedPlaybackQueue preparedPlaybackQueue;
	private IPositionedFileQueueProvider positionedFileQueueProvider;

	private ConnectableObservable<PositionedPlaybackFile> observableProxy;
	private Disposable fileChangedObservableConnection;
	private Disposable subscription;
	private ObservableEmitter<PositionedPlaybackFile> observableEmitter;

	public PlaybackPlaylistStateManager(IPlaybackPreparerProvider playbackPreparerProvider, Iterable<IPositionedFileQueueProvider> positionedFileQueueProviders, INowPlayingRepository nowPlayingRepository, CachedFilePropertiesProvider cachedFilePropertiesProvider, float initialVolume) {
		this.playbackPreparerProvider = playbackPreparerProvider;
		this.nowPlayingRepository = nowPlayingRepository;
		this.positionedFileQueueProviders = Stream.of(positionedFileQueueProviders).collect(Collectors.toMap(IPositionedFileQueueProvider::isRepeating, fp -> fp));
		this.cachedFilePropertiesProvider = cachedFilePropertiesProvider;
		volume = initialVolume;
	}

	@Override
	public void subscribe(ObservableEmitter<PositionedPlaybackFile> e) throws Exception {
		observableEmitter = e;
	}

	public Promise<Observable<PositionedPlaybackFile>> startPlaylist(final List<ServiceFile> playlist, final int playlistPosition, final int filePosition) {
		logger.info("Starting playback");

		this.playlist = playlist;

		final Promise<Observable<PositionedPlaybackFile>> observablePromise =
			updateLibraryPlaylistPositions(playlistPosition, filePosition)
				.then(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));

		observablePromise.error(runCarelessly(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	public Promise<Observable<PositionedPlaybackFile>> skipToNext() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> changePosition(getNextPosition(np.playlistPosition, np.playlist), 0));
	}

	private static int getNextPosition(int startingPosition, Collection<ServiceFile> playlist) {
		return startingPosition < playlist.size() - 1 ? startingPosition + 1 : 0;
	}

	public Promise<Observable<PositionedPlaybackFile>> skipToPrevious() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> changePosition(getPreviousPosition(np.playlistPosition), 0));
	}

	private static int getPreviousPosition(int startingPosition) {
		return startingPosition > 0 ? startingPosition - 1 : 0;
	}

	public synchronized Promise<Observable<PositionedPlaybackFile>> changePosition(final int playlistPosition, final int filePosition) {
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

		if (isPlaying) {
			final Promise<Observable<PositionedPlaybackFile>> observablePromise =
				nowPlayingPromise
					.then(this::initializePreparedPlaybackQueue)
					.then(q -> startPlayback(q, filePosition));

			observablePromise.error(runCarelessly(this::uncaughtExceptionHandler));

			return observablePromise;
		}

		final Promise<Observable<PositionedPlaybackFile>> singleFileChangeObservablePromise =
			nowPlayingPromise
				.thenPromise(np -> {
					final ServiceFile serviceFile = np.playlist.get(playlistPosition);

					return
						this.cachedFilePropertiesProvider
							.promiseFileProperties(serviceFile.getKey())
							.then(fileProperties -> {
								final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(fileProperties);

								final PositionedPlaybackFile positionedPlaybackFile = new PositionedPlaybackFile(
									playlistPosition,
									new EmptyPlaybackHandler(duration),
									serviceFile);

								observableEmitter.onNext(positionedPlaybackFile);

								return Observable.just(positionedPlaybackFile);
							});
				});

		singleFileChangeObservablePromise.error(runCarelessly(e -> logger.warn("There was an error getting the serviceFile properties", e)));

		return singleFileChangeObservablePromise;
	}

	void playRepeatedly() {
		persistLibraryRepeating(true);

		updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(true));
	}

	void playToCompletion() {
		persistLibraryRepeating(false);

		updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(false));
	}

	Promise<Observable<PositionedPlaybackFile>> resume() {
		if (playlistPlayer != null) {
			playlistPlayer.resume();

			isPlaying = true;

			saveStateToLibrary();

			return new Promise<>(observableProxy);
		}

		final Promise<Observable<PositionedPlaybackFile>> observablePromise =
			restorePlaylistFromStorage()
				.then(np -> startPlayback(initializePreparedPlaybackQueue(np), np.filePosition));

		observablePromise.error(runCarelessly(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	public void pause() {
		if (playlistPlayer != null)
			playlistPlayer.pause();

		isPlaying = false;

		saveStateToLibrary();
	}

	public boolean isPlaying() {
		return isPlaying;
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

		observableProxy = Observable.create(playlistPlayer).publish();

		subscription = observableProxy.subscribe(
			p -> {
				isPlaying = true;
				positionedPlaybackFile = p;

				if (observableEmitter != null)
					observableEmitter.onNext(p);

				saveStateToLibrary();
			},
			this::uncaughtExceptionHandler,
			() -> {
				isPlaying = false;
				saveStateToLibrary();
				changePosition(0, 0);
			});

		fileChangedObservableConnection = observableProxy.connect();
		isPlaying = true;

		return observableProxy;
	}

	Promise<NowPlaying> addFile(ServiceFile serviceFile) {
		final Promise<NowPlaying> nowPlayingPromise =
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> {
					np.playlist.add(serviceFile);
					return nowPlayingRepository.updateNowPlaying(np);
				});

		if (playlist == null) return nowPlayingPromise;

		playlist.add(serviceFile);

		updatePreparedFileQueueFromState();
		return nowPlayingPromise;
	}

	Promise<NowPlaying> removeFileAtPosition(int position) {
		final Promise<NowPlaying> libraryUpdatePromise =
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> {
					np.playlist.remove(position);
					return nowPlayingRepository.updateNowPlaying(np);
				});

		if (playlist == null) return libraryUpdatePromise;

		playlist.remove(position);

		updatePreparedFileQueueFromState();

		return libraryUpdatePromise;
	}

	void setVolume(float volume) {
		this.volume = volume;

		if (playlistPlayer != null)
			playlistPlayer.setVolume(volume);
	}

	private void updatePreparedFileQueueUsingState(IPositionedFileQueueProvider fileQueueProvider) {
		positionedFileQueueProvider = fileQueueProvider;

		updatePreparedFileQueueFromState();
	}

	private void updatePreparedFileQueueFromState() {
		if (preparedPlaybackQueue != null && playlist != null && positionedPlaybackFile != null)
			preparedPlaybackQueue.updateQueue(positionedFileQueueProvider.provideQueue(playlist, positionedPlaybackFile.getPlaylistPosition() + 1));
	}

	private Promise<NowPlaying> updateLibraryPlaylistPositions(final int playlistPosition, final int filePosition) {
		final Promise<NowPlaying> nowPlayingPromise =
			playlist != null
				? nowPlayingRepository.getNowPlaying()
				: restorePlaylistFromStorage();

		return
			nowPlayingPromise
				.thenPromise(np -> {
					np.playlist = playlist;
					np.playlistPosition = playlistPosition;
					np.filePosition = filePosition;
					return nowPlayingRepository.updateNowPlaying(np);
				});
	}

	private Promise<NowPlaying> restorePlaylistFromStorage() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.then(np -> {
					this.playlist = np.playlist;
					return np;
				});
	}

	private PreparedPlaybackQueue initializePreparedPlaybackQueue(NowPlaying nowPlaying) throws IOException {
		if (preparedPlaybackQueue != null)
			preparedPlaybackQueue.close();

		final int startPosition = Math.max(nowPlaying.playlistPosition, 0);

		if (positionedFileQueueProvider == null) {
			positionedFileQueueProvider = positionedFileQueueProviders.get(nowPlaying.isRepeating);
		}

		return
			preparedPlaybackQueue =
				new PreparedPlaybackQueue(
					this.playbackPreparerProvider.providePlaybackPreparer(),
					positionedFileQueueProvider.provideQueue(playlist, startPosition));
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

		isPlaying = false;

		if (preparedPlaybackQueue != null) preparedPlaybackQueue.close();
	}
}