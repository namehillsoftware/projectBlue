package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.MediaPlayerInitializer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.MediaPlayerPlaybackPreparerTaskFactory;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.IPositionedFileQueueProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.queues.PreparedPlaybackQueue;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
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

	PlaybackPlaylistStateManager(Context context, int libraryId, IPositionedFileQueueProvider positionedFileQueueProvider) {
		this.context = context;
		this.libraryId = libraryId;
		this.positionedFileQueueProvider = positionedFileQueueProvider;
	}

	private IPromise<Library> restorePlaylistFromStorage() {
		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library -> {
					if (library.getSavedTracksString() == null)
						return new PassThroughPromise<>(library);

					return
						FileStringListUtilities
							.promiseParsedFileStringList(library.getSavedTracksString())
							.then(playlist -> {
								this.playlist = playlist;
								return library;
							});
				});
	}

	IPromise<Observable<PositionedPlaybackFile>> startPlaylist(final List<IFile> playlist, final int playlistPosition, final int filePosition) {
		logger.info("Starting playback");

		if (playlistPlayer != null) {
			try {
				playlistPlayer.close();
			} catch (IOException e) {
				logger.error("There was an error closing the playlist player", e);
			}
		}

		this.playlist = playlist;

		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			updateLibraryPlaylist(playlistPosition, filePosition)
				.then(this::initializePreparedPlaybackQueue)
				.then((q, resolve, reject) -> {
					try {
						resolve.withResult(startPlayback(q, filePosition));
					} catch (IOException e) {
						reject.withError(e);
					}
				});

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

		IPromise<Library> libraryPromise = updateLibraryPlaylist(playlistPosition, filePosition);

		if (!wasPlaying)
			return new ExpectedPromise<>(Observable::empty);

		logger.info("Position changed");
		final IPromise<Observable<PositionedPlaybackFile>> observablePromise =
			libraryPromise
				.then(this::initializePreparedPlaybackQueue)
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
				.then((library) -> startPlayback(initializePreparedPlaybackQueue(library), library.getNowPlayingId()));

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
		fileChangedObservableConnection = observableProxy.connect();

		observableProxy.subscribe(p -> {
			positionedPlaybackFile = p;
			saveStateToLibrary();
		}, this::uncaughtExceptionHandler);

		return observableProxy;
	}

	IPromise<Library> addFile(IFile file) {
		final IPromise<Library> libraryUpdatePromise =
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise((result) -> {
					String newFileString = result.getSavedTracksString();
					if (!newFileString.endsWith(";")) newFileString += ";";
					newFileString += file.getKey() + ";";
					result.setSavedTracksString(newFileString);

					return LibrarySession.saveLibrary(context, result);
				});

		if (playlist == null) return libraryUpdatePromise;

		playlist.add(file);

		if (preparedPlaybackQueue == null) return libraryUpdatePromise;

		preparedPlaybackQueue.updateQueue(positionedFileQueueGenerator.resultFrom(playlist, positionedPlaybackFile.getPosition()));
		return libraryUpdatePromise;
	}

	IPromise<Library> removeFileAtPosition(int position) {
		final IPromise<Library> libraryUpdatePromise =
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(library ->
					FileStringListUtilities
						.promiseParsedFileStringList(library.getSavedTracksString())
						.thenPromise(savedTracks -> {
							savedTracks.remove(position);
							return FileStringListUtilities.promiseSerializedFileStringList(savedTracks);
						})
						.thenPromise(savedTracks -> {
							library.setSavedTracksString(savedTracks);

							return LibrarySession.saveLibrary(context, library);
						}));

		if (playlist == null) return libraryUpdatePromise;

		playlist.remove(position);

		if (preparedPlaybackQueue == null) return libraryUpdatePromise;

		final IPositionedFileQueue newPositionedFileQueue = positionedFileQueueGenerator.resultFrom(playlist, positionedPlaybackFile.getPosition());
		preparedPlaybackQueue.updateQueue(newPositionedFileQueue);

		return libraryUpdatePromise;
	}

	private IPromise<Library> updateLibraryPlaylist(final int playlistPosition, final int filePosition) {
		return
			LibrarySession
				.getLibrary(context, libraryId)
				.thenPromise(result ->
					FileStringListUtilities
						.promiseSerializedFileStringList(playlist)
						.thenPromise(playlistString -> {
							result.setSavedTracksString(playlistString);
							result.setNowPlayingId(playlistPosition);
							result.setNowPlayingProgress(filePosition);

							return LibrarySession.saveLibrary(context, result);
						}));
	}

	void setVolume(float volume) {
		this.volume = volume;

		if (playlistPlayer != null)
			playlistPlayer.setVolume(volume);
	}

	private PreparedPlaybackQueue initializePreparedPlaybackQueue(Library library) {
		if (preparedPlaybackQueue != null) {
			try {
				preparedPlaybackQueue.close();
			} catch (IOException e) {
				logger.warn("There was an error closing the prepared playback queue", e);
			}
		}

		final IFileUriProvider uriProvider = new BestMatchUriProvider(context, SessionConnection.getSessionConnectionProvider(), library);

		final int startPosition = Math.max(library.getNowPlayingId(), 0);

		positionedFileQueueGenerator =
			library.isRepeating()
				? positionedFileQueueProvider::getCyclicalQueue
				: positionedFileQueueProvider::getCompletableQueue;

		preparedPlaybackQueue =
			new PreparedPlaybackQueue(
				new MediaPlayerPlaybackPreparerTaskFactory(
					uriProvider,
					new MediaPlayerInitializer(context, library)),
				positionedFileQueueGenerator.resultFrom(playlist, startPosition));

		return preparedPlaybackQueue;
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
	}
}