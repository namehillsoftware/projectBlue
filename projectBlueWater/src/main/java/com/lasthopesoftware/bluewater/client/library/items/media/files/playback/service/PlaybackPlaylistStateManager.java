package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.INowPlayingRepository;
import com.lasthopesoftware.bluewater.client.library.items.media.files.nowplaying.storage.NowPlaying;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.MediaPlayerInitializer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.MediaPlayerPlaybackPreparer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.IFileUriProvider;
import com.lasthopesoftware.bluewater.client.library.items.playlists.playback.PlaylistPlayer;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.bluewater.client.library.repository.LibrarySession;
import com.lasthopesoftware.promises.ExpectedPromise;
import com.lasthopesoftware.promises.IPromise;
import com.lasthopesoftware.promises.PassThroughPromise;
import com.vedsoft.futures.callables.TwoParameterFunction;
import com.vedsoft.futures.callables.VoidFunc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;

class PlaybackPlaylistStateManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(PlaybackPlaylistStateManager.class);

	private final Context context;
	private final IFileUriProvider fileUriProvider;
	private final INowPlayingRepository nowPlayingRepository;
	private final int libraryId;
	private final IPositionedFileQueueProvider positionedFileQueueProvider;

	private PositionedPlaybackFile positionedPlaybackFile;
	private PlaylistPlayer playlistPlayer;
	private List<IFile> playlist;
	private PreparedPlaybackQueue preparedPlaybackQueue;
	private ConnectableObservable<PositionedPlaybackFile> observableProxy;
	private Disposable fileChangedObservableConnection;
	private TwoParameterFunction<List<IFile>, Integer, IPositionedFileQueue> positionedFileQueueGenerator;
	private float volume;

	PlaybackPlaylistStateManager(Context context, IFileUriProvider fileUriProvider, IPositionedFileQueueProvider positionedFileQueueProvider, INowPlayingRepository nowPlayingRepository, int libraryId, float initialVolume) {
		this.context = context;
		this.fileUriProvider = fileUriProvider;
		this.nowPlayingRepository = nowPlayingRepository;
		this.libraryId = libraryId;
		this.positionedFileQueueProvider = positionedFileQueueProvider;
		volume = initialVolume;
	}

	IPromise<Observable<PositionedPlaybackFile>> startPlaylist(final List<IFile> playlist, final int playlistPosition, final int filePosition) {
		logger.info("Starting playback");

		this.playlist = playlist;

		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			updateLibraryPlaylist(playlistPosition, filePosition)
				.thenPromise(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));

		observablePromise.error(VoidFunc.runningCarelessly(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	IPromise<Observable<PositionedPlaybackFile>> skipToNext() {
		if (playlist != null && positionedPlaybackFile != null) {
			final int newPosition =  positionedPlaybackFile.getPosition();
			final int playlistSize = playlist.size();
			return changePosition(newPosition < playlistSize - 1 ? newPosition + 1 : 0, 0);
		}

		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library -> {
					final int newPosition =  library.getNowPlayingId();

					return
						FileStringListUtilities
							.promiseParsedFileStringList(library.getSavedTracksString())
							.thenPromise(savedTracks -> {
								final int playlistSize = savedTracks.size();
								return changePosition(newPosition < playlistSize - 1 ? newPosition + 1 : 0, 0);
							});
				});
	}

	IPromise<Observable<PositionedPlaybackFile>> skipToPrevious() {
		if (positionedPlaybackFile != null) {
			final int position =  positionedPlaybackFile.getPosition();
			return changePosition(position > 0 ? position - 1 : 0, 0);
		}

		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library -> {
					final int position =  library.getNowPlayingId();
					return changePosition(position > 0 ? position - 1 : 0, 0);
				});
	}

	IPromise<Observable<PositionedPlaybackFile>> changePosition(final int playlistPosition, final int filePosition) {
		final boolean wasPlaying = positionedPlaybackFile != null && positionedPlaybackFile.getPlaybackHandler().isPlaying();

		final IPromise<NowPlaying> nowPlayingPromise = updateLibraryPlaylist(playlistPosition, filePosition);

		if (!wasPlaying)
			return new ExpectedPromise<>(Observable::empty);

		logger.info("Position changed");
		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			nowPlayingPromise
				.thenPromise(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));

		observablePromise.error(VoidFunc.runningCarelessly(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	void playRepeatedly() {
		persistLibraryRepeating(true)
			.then(VoidFunc.runningCarelessly(
				library -> preparedPlaybackQueue.updateQueue((positionedFileQueueGenerator = positionedFileQueueProvider::getCyclicalQueue).resultFrom(playlist, library.getNowPlayingId()))));
	}

	void playToCompletion() {
		persistLibraryRepeating(false)
			.then(VoidFunc.runningCarelessly(
				library ->	preparedPlaybackQueue.updateQueue((positionedFileQueueGenerator = positionedFileQueueProvider::getCompletableQueue).resultFrom(playlist, library.getNowPlayingId()))));
	}

	IPromise<Observable<PositionedPlaybackFile>> resume() {
		if (playlistPlayer != null) {
			playlistPlayer.resume();

			saveStateToLibrary();

			return new PassThroughPromise<>(observableProxy);
		}

		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			restorePlaylistFromStorage()
				.thenPromise((np) -> initializePreparedPlaybackQueue(np).then(queue -> startPlayback(queue, np.filePosition)));

		observablePromise.error(VoidFunc.runningCarelessly(this::uncaughtExceptionHandler));

		return observablePromise;
	}

	public void pause() {
		if (playlistPlayer != null)
			playlistPlayer.pause();

		saveStateToLibrary();
	}

	public boolean isPlaying() {
		return playlistPlayer != null && playlistPlayer.isPlaying();
	}

	private Observable<PositionedPlaybackFile> startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) throws IOException {
		if (fileChangedObservableConnection != null && !fileChangedObservableConnection.isDisposed())
			fileChangedObservableConnection.dispose();

		if (playlistPlayer != null)
			playlistPlayer.close();

		playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, filePosition);
		playlistPlayer.setVolume(volume);

		observableProxy = Observable.create(playlistPlayer).publish();

		observableProxy.subscribe(p -> {
			positionedPlaybackFile = p;
			saveStateToLibrary();
		}, this::uncaughtExceptionHandler);

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

		if (preparedPlaybackQueue == null) return nowPlayingPromise;

		preparedPlaybackQueue.updateQueue(positionedFileQueueGenerator.resultFrom(playlist, positionedPlaybackFile.getPosition()));
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

		if (preparedPlaybackQueue == null) return libraryUpdatePromise;

		final IPositionedFileQueue newPositionedFileQueue = positionedFileQueueGenerator.resultFrom(playlist, positionedPlaybackFile.getPosition());
		preparedPlaybackQueue.updateQueue(newPositionedFileQueue);

		return libraryUpdatePromise;
	}

	private IPromise<NowPlaying> updateLibraryPlaylist(final int playlistPosition, final int filePosition) {
		return
			nowPlayingRepository
				.getNowPlaying()
				.thenPromise(np -> {
					final NowPlaying newNowPlaying = new NowPlaying(playlist, playlistPosition, filePosition, np.isRepeating);
					return nowPlayingRepository.updateNowPlaying(newNowPlaying);
				});
	}

	void setVolume(float volume) {
		this.volume = volume;

		if (playlistPlayer != null)
			playlistPlayer.setVolume(volume);
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

		positionedFileQueueGenerator =
			nowPlaying.isRepeating
				? positionedFileQueueProvider::getCyclicalQueue
				: positionedFileQueueProvider::getCompletableQueue;

		return
			LibrarySession
				.getLibrary(context, libraryId)
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

	private IPromise<Library> persistLibraryRepeating(boolean isRepeating) {
		return
			LibrarySession
				.getLibrary(context, libraryId)
				.then(result -> {
					result.setRepeating(isRepeating);

					LibrarySession.saveLibrary(context, result);

					return result;
				});
	}

	private void saveStateToLibrary() {
		if (playlist == null) return;

		LibrarySession
			.getLibrary(context, libraryId)
			.thenPromise(library ->
				FileStringListUtilities
					.promiseSerializedFileStringList(playlist)
					.thenPromise(savedTracksString -> {
						library.setSavedTracksString(savedTracksString);

						if (positionedPlaybackFile != null) {
							library.setNowPlayingId(positionedPlaybackFile.getPosition());
							library.setNowPlayingProgress(positionedPlaybackFile.getPlaybackHandler().getCurrentPosition());
						}

						return LibrarySession.saveLibrary(context, library);
					}));
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