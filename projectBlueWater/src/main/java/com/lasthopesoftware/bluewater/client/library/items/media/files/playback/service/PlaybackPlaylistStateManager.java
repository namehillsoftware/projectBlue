package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.access.ISpecificLibraryProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.MediaPlayerInitializer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.MediaPlayerPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.CachedFilePropertiesProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.properties.FilePropertyHelpers;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.Promise;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.lazyj.AbstractSynchronousLazy;
import com.vedsoft.lazyj.ILazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

import static com.vedsoft.futures.callables.VoidFunc.runningCarelessly;

class PlaybackPlaylistStateManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaybackPlaylistStateManager.class);

	private final Context context;
	private final IConnectionProvider connectionProvider;
	private final IFileUriProvider fileUriProvider;
	private final INowPlayingRepository nowPlayingRepository;
	private final IPositionedFileQueueProvider positionedFileQueueProvider;
	private final ILazy<IPromise<Library>> lazyLibraryPromise;

	private PositionedPlaybackFile positionedPlaybackFile;
	private PlaylistPlayer playlistPlayer;
	private List<IFile> playlist;
	private float volume;
	private boolean isPlaying;

	private PreparedPlaybackQueue preparedPlaybackQueue;
	private TwoParameterFunction<List<IFile>, Integer, IPositionedFileQueue> positionedFileQueueGenerator;

	private ConnectableObservable<PositionedPlaybackFile> observableProxy;
	private Disposable fileChangedObservableConnection;
	private Disposable subscription;

	PlaybackPlaylistStateManager(Context context, IConnectionProvider connectionProvider, IFileUriProvider fileUriProvider, IPositionedFileQueueProvider positionedFileQueueProvider, INowPlayingRepository nowPlayingRepository, ISpecificLibraryProvider libraryProvider, float initialVolume) {
		this.context = context;
		this.connectionProvider = connectionProvider;
		this.fileUriProvider = fileUriProvider;
		this.nowPlayingRepository = nowPlayingRepository;
		this.positionedFileQueueProvider = positionedFileQueueProvider;
		this.lazyLibraryPromise = new AbstractSynchronousLazy<IPromise<Library>>() {
			@Override
			protected IPromise<Library> initialize() throws Exception {
				return libraryProvider.getLibrary();
			}
		};
		volume = initialVolume;
	}

	IPromise<Observable<PositionedPlaybackFile>> startPlaylist(final List<IFile> playlist, final int playlistPosition, final int filePosition) {
		logger.info("Starting playback");

		this.playlist = playlist;

		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			updateLibraryPlaylistPositions(playlistPosition, filePosition)
				.thenPromise(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));

		observablePromise.error(runningCarelessly(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	IPromise<Observable<PositionedPlaybackFile>> skipToNext() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> changePosition(getNextPosition(np.playlistPosition, np.playlist), 0));
	}

	private static int getNextPosition(int startingPosition, Collection<IFile> playlist) {
		return startingPosition < playlist.size() - 1 ? startingPosition + 1 : 0;
	}

	IPromise<Observable<PositionedPlaybackFile>> skipToPrevious() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> changePosition(getPreviousPosition(np.playlistPosition), 0));
	}

	private static int getPreviousPosition(int startingPosition) {
		return startingPosition > 0 ? startingPosition - 1 : 0;
	}

	synchronized IPromise<Observable<PositionedPlaybackFile>> changePosition(final int playlistPosition, final int filePosition) {
		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
			fileChangedObservableConnection.dispose();

		if (subscription != null)
			subscription.dispose();

		final IPromise<NowPlaying> nowPlayingPromise =
			updateLibraryPlaylistPositions(playlistPosition, filePosition)
				.then(np -> {
					logger.info("Position changed");
					return np;
				});

		if (isPlaying) {
			final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
				nowPlayingPromise
					.thenPromise(this::initializePreparedPlaybackQueue)
					.then(q -> startPlayback(q, filePosition));

			observablePromise.error(runningCarelessly(this::uncaughtExceptionHandler));

			return observablePromise;
		}

		final IPromise<Observable<PositionedPlaybackFile>> singleFileChangeObservablePromise =
			nowPlayingPromise
				.then((np, resolve, reject, onCancelled) -> {
					final IFile file = np.playlist.get(playlistPosition);
					final CachedFilePropertiesProvider filePropertiesProvider = new CachedFilePropertiesProvider(connectionProvider, file.getKey());
					onCancelled.runWith(() -> {
						filePropertiesProvider.cancel();
						reject.withError(new InterruptedException());
					});

					filePropertiesProvider
						.onComplete(properties -> {
							final int duration = FilePropertyHelpers.parseDurationIntoMilliseconds(properties);

							resolve.withResult(
								Observable.just(
									new PositionedPlaybackFile(
										playlistPosition,
										new EmptyPlaybackHandler(duration),
										file)));
						})
						.onError(e -> {
							reject.withError(e);
							return true;
						});

					filePropertiesProvider.execute();
				});

		singleFileChangeObservablePromise.error(runningCarelessly(e -> logger.warn("There was an error getting the file properties", e)));

		return singleFileChangeObservablePromise;
	}

	void playRepeatedly() {
		persistLibraryRepeating(true);

		updatePreparedFileQueueUsingState(positionedFileQueueProvider::getCyclicalQueue);
	}

	void playToCompletion() {
		persistLibraryRepeating(false);

		updatePreparedFileQueueUsingState(positionedFileQueueProvider::getCompletableQueue);
	}

	IPromise<Observable<PositionedPlaybackFile>> resume() {
		if (playlistPlayer != null) {
			playlistPlayer.resume();

			isPlaying = true;

			saveStateToLibrary();

			return new Promise<>(observableProxy);
		}

		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			restorePlaylistFromStorage()
				.thenPromise(np -> initializePreparedPlaybackQueue(np).then(queue -> startPlayback(queue, np.filePosition)));

		observablePromise.error(runningCarelessly(this::uncaughtExceptionHandler));

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
				saveStateToLibrary();
			},
			this::uncaughtExceptionHandler,
			() -> {
				isPlaying = false;
				saveStateToLibrary();
			});

		fileChangedObservableConnection = observableProxy.connect();

		return observableProxy;
	}

	IPromise<NowPlaying> addFile(IFile file) {
		final IPromise<NowPlaying> nowPlayingPromise =
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> {
					np.playlist.add(file);
					return nowPlayingRepository.updateNowPlaying(np);
				});

		if (playlist == null) return nowPlayingPromise;

		playlist.add(file);

		updatePreparedFileQueueFromState();
		return nowPlayingPromise;
	}

	IPromise<NowPlaying> removeFileAtPosition(int position) {
		final IPromise<NowPlaying> libraryUpdatePromise =
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

	private void updatePreparedFileQueueUsingState(TwoParameterFunction<List<IFile>, Integer, IPositionedFileQueue> newFileQueueGenerator) {
		positionedFileQueueGenerator = newFileQueueGenerator;

		updatePreparedFileQueueFromState();
	}

	private void updatePreparedFileQueueFromState() {
		if (preparedPlaybackQueue != null && playlist != null && positionedPlaybackFile != null)
			preparedPlaybackQueue.updateQueue(positionedFileQueueGenerator.resultFrom(playlist, positionedPlaybackFile.getPosition() + 1));
	}

	private IPromise<NowPlaying> updateLibraryPlaylistPositions(final int playlistPosition, final int filePosition) {
		return
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> {
					np.playlistPosition = playlistPosition;
					np.filePosition = filePosition;
					return nowPlayingRepository.updateNowPlaying(np);
				});
	}

	private IPromise<NowPlaying> restorePlaylistFromStorage() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.then(np -> {
					this.playlist = np.playlist;
					return np;
				});
	}

	private IPromise<PreparedPlaybackQueue> initializePreparedPlaybackQueue(NowPlaying nowPlaying) throws IOException {
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
			lazyLibraryPromise
				.getObject()
				.then(library -> {
					preparedPlaybackQueue =
						new PreparedPlaybackQueue(
							new MediaPlayerPlaybackPreparer(
								fileUriProvider,
								new MediaPlayerInitializer(context, library)),
							positionedFileQueueGenerator.resultFrom(playlist, startPosition));

					return preparedPlaybackQueue;
				});
	}

	private IPromise<NowPlaying> persistLibraryRepeating(boolean isRepeating) {
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
					np.playlistPosition = positionedPlaybackFile.getPosition();
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