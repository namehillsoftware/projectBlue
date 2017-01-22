package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service;

import android.content.Context;

import com.lasthopesoftware.bluewater.client.connection.SessionConnection;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PositionedPlaybackFile;
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
import com.lasthopesoftware.promises.IPromise;
import com.vedsoft.futures.callables.VoidFunc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import io.reactivex.Observable;

/**
 * Created by david on 1/22/17.
 */

public class PlaybackPlaylistStateManager {

	private static final Logger logger = LoggerFactory.getLogger(PlaybackPlaylistStateManager.class);

	private final Context context;
	private final IPositionedFileQueueProvider positionedFileQueueProvider;
	private final Object syncPlaylistControllerObject = new Object();

	private PositionedPlaybackFile positionedPlaybackFile;
	private PlaylistPlayer playlistPlayer;
	private List<IFile> playlist;
	private PreparedPlaybackQueue preparedPlaybackQueue;

	public PlaybackPlaylistStateManager(Context context, IPositionedFileQueueProvider positionedFileQueueProvider) {
		this.context = context;
		this.positionedFileQueueProvider = positionedFileQueueProvider;
	}

	public IPromise<Boolean> restorePlaylistFromStorage() {
		return
			LibrarySession
				.getActiveLibrary(context)
				.then((library, resolve, reject) -> {
					if (library == null) {
						resolve.withResult(false);
						return;
					}

					if (library.getSavedTracksString() == null || library.getSavedTracksString().isEmpty()) {
						resolve.withResult(false);
						return;
					}

					FileStringListUtilities
						.promiseParsedFileStringList(library.getSavedTracksString())
						.then(playlist -> {
							this.playlist = playlist;
							return library;
						})
						.then(VoidFunc.running(playlistPlayer -> resolve.withResult(true)))
						.error(VoidFunc.running(reject::withError));
				});
	}

	public IPromise<Observable<PositionedPlaybackFile>> startPlaylist(final List<IFile> playlist, final int playlistPosition, final int filePosition) {
//		notifyStartingService();

		logger.info("Starting playback");

		if (playlistPlayer != null) {
			try {
				playlistPlayer.close();
			} catch (IOException e) {
				logger.error("There was an error closing the playlist player", e);
			}
		}

		this.playlist = playlist;

		return
			updateLibraryPlaylist(playlistPosition, filePosition)
				.then(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));
//			.error(VoidFunc.running(this::uncaughtExceptionHandler));
	}

	public void changePosition(final int playlistPosition, final int filePosition) {
		boolean wasPlaying = positionedPlaybackFile != null && positionedPlaybackFile.getPlaybackHandler().isPlaying();

		IPromise<Library> libraryPromise = updateLibraryPlaylist(playlistPosition, filePosition);

		if (wasPlaying) {
			libraryPromise
				.then(this::initializePreparedPlaybackQueue)
				.then(q -> startPlayback(q, filePosition));
//				.error(VoidFunc.running(this::uncaughtExceptionHandler));
		}

		logger.info("Position changed");
	}

	public void playRepeatedly() {
		persistLibraryRepeating(true)
			.then(VoidFunc.running(library -> {
				final IPositionedFileQueue cyclicalQueue = positionedFileQueueProvider.getCyclicalQueue(playlist, library.getNowPlayingId());

				preparedPlaybackQueue.updateQueue(cyclicalQueue);
			}));
	}

	public void playToCompletion() {
		persistLibraryRepeating(false)
			.then(VoidFunc.running(library -> {
				if (playlistPlayer == null) return;

				final IPositionedFileQueue cyclicalQueue = positionedFileQueueProvider.getCompletableQueue(playlist, library.getNowPlayingId());

				preparedPlaybackQueue.updateQueue(cyclicalQueue);
			}));
	}

	public Observable<PositionedPlaybackFile> resume() {
		if (playlistPlayer == null) return Observable.empty();

		playlistPlayer.resume();

		saveStateToLibrary();

		return Observable.create(playlistPlayer);
	}

	public void pause() {
		if (playlistPlayer != null)
			playlistPlayer.pause();

		saveStateToLibrary();
	}

	private Observable<PositionedPlaybackFile> startPlayback(PreparedPlaybackQueue preparedPlaybackQueue, final int filePosition) {
		return Observable.create((playlistPlayer = new PlaylistPlayer(preparedPlaybackQueue, filePosition)));
	}

	private IPromise<Library> updateLibraryPlaylist(final int playlistPosition, final int filePosition) {
		return
			LibrarySession
				.getActiveLibrary(context)
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

		final IPositionedFileQueue positionedFileQueue =
			library.isRepeating()
				? positionedFileQueueProvider.getCyclicalQueue(playlist, startPosition)
				: positionedFileQueueProvider.getCompletableQueue(playlist, startPosition);

		preparedPlaybackQueue =
			new PreparedPlaybackQueue(
				new MediaPlayerPlaybackPreparerTaskFactory(
					uriProvider,
					new MediaPlayerInitializer(context, library)),
				positionedFileQueue);

		return preparedPlaybackQueue;
	}

	private IPromise<Library> persistLibraryRepeating(boolean isRepeating) {
		return
			LibrarySession
				.getActiveLibrary(context)
				.then(result -> {
					result.setRepeating(isRepeating);

					LibrarySession.saveLibrary(context, result);

					return result;
				});
	}

	private void saveStateToLibrary() {
		if (playlist == null) return;

		LibrarySession
			.getActiveLibrary(context)
			.then(VoidFunc.running(library -> {
				FileStringListUtilities
					.promiseSerializedFileStringList(playlist)
					.then(VoidFunc.running(savedTracksString -> {
						library.setSavedTracksString(savedTracksString);

						if (positionedPlaybackFile != null) {
							library.setNowPlayingId(positionedPlaybackFile.getPosition());
							library.setNowPlayingProgress(positionedPlaybackFile.getPlaybackHandler().getCurrentPosition());
						}

						LibrarySession.saveLibrary(context, library);
					}));
			}));
	}
}