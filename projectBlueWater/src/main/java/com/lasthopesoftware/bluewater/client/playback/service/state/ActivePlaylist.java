package com.lasthopesoftware.bluewater.client.playback.service.state;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.playback.service.PlaybackPlaylistStateManager;
import com.lasthopesoftware.promises.EmptyMessenger;
import com.lasthopesoftware.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

import static com.vedsoft.futures.callables.VoidFunc.runCarelessly;

public final class ActivePlaylist implements IStartedPlaylist, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaybackPlaylistStateManager.class);

	private final IPlaybackPreparerProvider playbackPreparerProvider;
	private final INowPlayingRepository nowPlayingRepository;
	private final Map<Boolean, IPositionedFileQueueProvider> positionedFileQueueProviders;
	private IPositionedFileQueueProvider positionedFileQueueProvider;

	private PositionedPlaybackFile positionedPlaybackFile;
	private PlaylistPlayer playlistPlayer;
	private List<ServiceFile> playlist;
	private float volume;

	private PreparedPlaybackQueue preparedPlaybackQueue;

	private Disposable fileChangedObservableConnection;
	private Disposable subscription;
	private ConnectableObservable<PositionedPlaybackFile> observableProxy;

	public ActivePlaylist(IPlaybackPreparerProvider playbackPreparerProvider, INowPlayingRepository nowPlayingRepository, Iterable<IPositionedFileQueueProvider> positionedFileQueueProviders, List<ServiceFile> playlist) {
		this.playbackPreparerProvider = playbackPreparerProvider;
		this.nowPlayingRepository = nowPlayingRepository;
		this.positionedFileQueueProviders = Stream.of(positionedFileQueueProviders).collect(Collectors.toMap(IPositionedFileQueueProvider::isRepeating, fp -> fp));
		this.playlist = playlist;
	}

	@Override
	public Promise<IPausedPlaylist> pause() {
		return Promise.empty();
	}

	@Override
	public Promise<IStartedPlaylist> playRepeatedly() {
		final Promise<?> persistLibraryRepeating = persistLibraryRepeating(true);

		updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(true));

		return persistLibraryRepeating.then(o -> this);
	}

	@Override
	public Promise<IStartedPlaylist> playToCompletion() {
		final Promise<?> persistLibraryRepeating = persistLibraryRepeating(false);

		updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(false));

		return persistLibraryRepeating.then(o -> this);
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
						.then(ActivePlaylist.this::initializePreparedPlaybackQueue)
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
	public Observable<PositionedPlaybackFile> observePosition() {
		return this.observableProxy;
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
		return
			nowPlayingRepository
				.getNowPlaying()
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

		observableProxy = Observable.create(playlistPlayer).replay(1);

		subscription = observableProxy.subscribe(
			p -> {
				positionedPlaybackFile = p;

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

		if (positionedFileQueueProvider == null)
			positionedFileQueueProvider = positionedFileQueueProviders.get(nowPlaying.isRepeating);

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
		if (preparedPlaybackQueue != null) preparedPlaybackQueue.close();
	}
}
