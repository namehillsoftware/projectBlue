package com.lasthopesoftware.bluewater.client.playback.state;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.playback.file.EmptyPlaybackHandler;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedFile;
import com.lasthopesoftware.bluewater.client.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.playback.file.preparation.IPlaybackPreparerProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.playback.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.playback.queues.PreparedPlaybackQueueResourceManagement;
import com.lasthopesoftware.bluewater.client.playback.state.bootstrap.IStartPlayback;
import com.lasthopesoftware.bluewater.shared.observables.SwitchableObservableSource;
import com.lasthopesoftware.messenger.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

public class PlaylistManager implements IChangePlaylistPosition, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(PlaylistManager.class);

	private final PreparedPlaybackQueueResourceManagement preparedPlaybackQueueResourceManagement;
	private final IStartPlayback playbackBootstrapper;
	private final INowPlayingRepository nowPlayingRepository;
	private final Map<Boolean, IPositionedFileQueueProvider> positionedFileQueueProviders;

	private PositionedPlaybackFile positionedPlaybackFile;
	private List<ServiceFile> playlist;
	private boolean isPlaying;

	private Disposable playbackSubscription;
	private IActivePlayer activePlayer;
	private SwitchableObservableSource<PositionedPlaybackFile> switchableObservableSource;
	private Observable<PositionedPlaybackFile> switchableObservable;

	public PlaylistManager(IPlaybackPreparerProvider playbackPreparerProvider, Iterable<IPositionedFileQueueProvider> positionedFileQueueProviders, INowPlayingRepository nowPlayingRepository, IStartPlayback playbackBootstrapper) {
		this.nowPlayingRepository = nowPlayingRepository;
		this.positionedFileQueueProviders = Stream.of(positionedFileQueueProviders).collect(Collectors.toMap(IPositionedFileQueueProvider::isRepeating, fp -> fp));
		preparedPlaybackQueueResourceManagement = new PreparedPlaybackQueueResourceManagement(playbackPreparerProvider);
		this.playbackBootstrapper = playbackBootstrapper;
	}

	public Promise<Observable<PositionedPlaybackFile>> startPlaylist(final List<ServiceFile> playlist, final int playlistPosition, final int filePosition) {
		logger.info("Starting playback");

		this.playlist = playlist;

		return
			updateLibraryPlaylistPositions(playlistPosition, filePosition)
				.then(this::startPlaybackFromNowPlaying)
				.then(this::startNewObservation);
	}

	public Promise<PositionedFile> skipToNext() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.eventually(np -> changePosition(getNextPosition(np.playlistPosition, np.playlist), 0));
	}

	private static int getNextPosition(int startingPosition, Collection<ServiceFile> playlist) {
		return startingPosition < playlist.size() - 1 ? startingPosition + 1 : 0;
	}

	public Promise<PositionedFile> skipToPrevious() {
		return
			nowPlayingRepository
				.getNowPlaying()
				.eventually(np -> changePosition(getPreviousPosition(np.playlistPosition), 0));
	}

	private static int getPreviousPosition(int startingPosition) {
		return startingPosition > 0 ? startingPosition - 1 : 0;
	}

	public synchronized Promise<PositionedFile> changePosition(final int playlistPosition, final int filePosition) {
		if (playbackSubscription != null)
			playbackSubscription.dispose();

		activePlayer = null;

		final Promise<NowPlaying> nowPlayingPromise =
			updateLibraryPlaylistPositions(playlistPosition, filePosition)
				.then(np -> {
					logger.info("Position changed");
					return np;
				});

		if (!isPlaying) {
			return
				nowPlayingPromise
					.then(np -> {
						final ServiceFile serviceFile = np.playlist.get(playlistPosition);
						return new PositionedFile(playlistPosition, serviceFile);
					});
		}

		return nowPlayingPromise
			.eventually((nowPlaying) -> new Promise<>(messenger -> {
				final IPositionedFileQueueProvider queueProvider = positionedFileQueueProviders.get(nowPlaying.isRepeating);
				try {
					final PreparedPlaybackQueue preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(queueProvider.provideQueue(playlist, playlistPosition));
					final Observable<PositionedPlaybackFile> playbackFileObservable =
						startPlayback(preparedPlaybackQueue, filePosition);

					playbackFileObservable
						.firstElement()
						.subscribe(playbackFile -> messenger.sendResolution(playbackFile.asPositionedFile()));

					switchObservation(playbackFileObservable);
				} catch (Exception e) {
					messenger.sendRejection(e);
				}
			}));
	}

	public void playRepeatedly() {
		persistLibraryRepeating(true);

		updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(true));
	}

	public void playToCompletion() {
		persistLibraryRepeating(false);

		updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(false));
	}

	public Promise<Observable<PositionedPlaybackFile>> resume() {
		if (activePlayer != null) {
			activePlayer.resume();

			isPlaying = true;

			return new Promise<>(switchableObservable);
		}

		return
			restorePlaylistFromStorage()
				.then(this::startPlaybackFromNowPlaying)
				.then(this::switchObservation);
	}

	public void pause() {
		if (activePlayer != null)
			activePlayer.pause();

		isPlaying = false;

		if (positionedPlaybackFile != null)
			saveStateToLibrary(positionedPlaybackFile);
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	private Observable<PositionedPlaybackFile> startNewObservation(Observable<PositionedPlaybackFile> observable) throws Exception {
		if (switchableObservableSource != null)
			switchableObservableSource.close();

		switchableObservableSource = new SwitchableObservableSource<>(observable);
		switchableObservable = Observable.create(switchableObservableSource);
		return switchableObservable;
	}

	private Observable<PositionedPlaybackFile> switchObservation(Observable<PositionedPlaybackFile> observable) throws Exception {
		if (switchableObservableSource == null) {
			return startNewObservation(observable);
		}

		switchableObservableSource.switchSource(observable);
		return switchableObservable;
	}

	private Observable<PositionedPlaybackFile> startPlaybackFromNowPlaying(NowPlaying nowPlaying) throws IOException {
		final IPositionedFileQueueProvider positionedFileQueueProvider = positionedFileQueueProviders.get(nowPlaying.isRepeating);

		final IPositionedFileQueue fileQueue = positionedFileQueueProvider.provideQueue(nowPlaying.playlist, nowPlaying.playlistPosition);
		final PreparedPlaybackQueue preparedPlaybackQueue = preparedPlaybackQueueResourceManagement.initializePreparedPlaybackQueue(fileQueue);
		return startPlayback(preparedPlaybackQueue, nowPlaying.filePosition);
	}

	private Observable<PositionedPlaybackFile> startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException {
		if (playbackSubscription != null)
			playbackSubscription.dispose();

		activePlayer = playbackBootstrapper.startPlayback(preparedPlaybackQueue, filePosition);
		isPlaying = true;

		final ConnectableObservable<PositionedPlaybackFile> observable = activePlayer.observe();

		playbackSubscription = observable.subscribe(
			p -> {
				isPlaying = true;
				positionedPlaybackFile = p;

				saveStateToLibrary(p);
			});

		return observable
			.concatWith(Observable.create(e -> {
				isPlaying = false;
				changePosition(0, 0).then(positionedFile -> {
					e.onNext(new PositionedPlaybackFile(
						new EmptyPlaybackHandler(0),
						positionedFile));

					e.onComplete();

					return null;
				});
			}));
	}

	public Promise<NowPlaying> addFile(ServiceFile serviceFile) {
		return
			nowPlayingRepository
				.getNowPlaying()
				.eventually(np -> {
					np.playlist.add(serviceFile);

					final Promise<NowPlaying> nowPlayingPromise = nowPlayingRepository.updateNowPlaying(np);

					if (playlist == null) return nowPlayingPromise;

					playlist.add(serviceFile);

					updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(np.isRepeating));
					return nowPlayingPromise;
				});
	}

	public Promise<NowPlaying> removeFileAtPosition(int position) {
		return
			nowPlayingRepository
				.getNowPlaying()
				.eventually(np -> {
					np.playlist.remove(position);

					final Promise<NowPlaying> libraryUpdatePromise = nowPlayingRepository.updateNowPlaying(np);

					if (playlist == null) return libraryUpdatePromise;

					playlist.remove(position);

					updatePreparedFileQueueUsingState(positionedFileQueueProviders.get(np.isRepeating));

					return libraryUpdatePromise;
				});
	}

	private void updatePreparedFileQueueUsingState(IPositionedFileQueueProvider fileQueueProvider) {
		if (playlist != null && positionedPlaybackFile != null)
			preparedPlaybackQueueResourceManagement
				.tryUpdateQueue(fileQueueProvider.provideQueue(playlist, positionedPlaybackFile.getPlaylistPosition() + 1));
	}

	private Promise<NowPlaying> updateLibraryPlaylistPositions(final int playlistPosition, final int filePosition) {
		final Promise<NowPlaying> nowPlayingPromise =
			playlist != null
				? nowPlayingRepository.getNowPlaying()
				: restorePlaylistFromStorage();

		return
			nowPlayingPromise
				.eventually(np -> {
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

	private void saveStateToLibrary(PositionedPlaybackFile positionedPlaybackFile) {
		if (playlist == null) return;

		nowPlayingRepository
			.getNowPlaying()
			.eventually(np -> {
				np.playlist = playlist;

				if (positionedPlaybackFile != null) {
					np.playlistPosition = positionedPlaybackFile.getPlaylistPosition();
					np.filePosition = positionedPlaybackFile.getPlaybackHandler().getCurrentPosition();
				}

				return nowPlayingRepository.updateNowPlaying(np);
			});
	}

	@Override
	public void close() throws Exception {
		isPlaying = false;

		preparedPlaybackQueueResourceManagement.close();

		if (switchableObservableSource != null)
			switchableObservableSource.close();
	}
}