package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import android.content.Context;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.lasthopesoftware.bluewater.client.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.PlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.initialization.MediaPlayerInitializer;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.CyclicalQueuedMediaPlayerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.QueuedMediaPlayerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.uri.BestMatchUriProvider;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.lasthopesoftware.promises.IPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class PlaybackController {
	private final HashSet<OnNowPlayingChangeListener> mOnNowPlayingChangeListeners = new HashSet<>();
	private final HashSet<OnNowPlayingStartListener> mOnNowPlayingStartListeners = new HashSet<>();
	private final HashSet<OnNowPlayingStopListener> mOnNowPlayingStopListeners = new HashSet<>();
	private final HashSet<OnNowPlayingPauseListener> mOnNowPlayingPauseListeners = new HashSet<>();
	private final HashSet<OnPlaylistStateControlErrorListener> mOnPlaylistStateControlErrorListeners = new HashSet<>();

	private final Context context;
	private final Library library;
	private final ConnectionProvider connectionProvider;
	private final ArrayList<IFile> playlist;
	private int currentFilePos = -1;
	private IPlaybackFile mCurrentPlaybackFile, mNextPlaybackFile;

	private float mVolume = 1.0f;
	private boolean isRepeating = false;
	private boolean isPlaying = false;

	private static final Logger mLogger = LoggerFactory.getLogger(PlaybackController.class);

	private IPreparedPlaybackFileProvider preparedPlaybackFileProvider;
	private IPlaybackHandler playbackHandler;
	private IPromise<IPlaybackHandler> preparingPlaybackFile;
	private int preparingFileIndex;

	public PlaybackController(final Context context, final Library library, final ConnectionProvider connectionProvider, final String playlistString) {
		this.context = context;
		this.library = library;
		this.connectionProvider = connectionProvider;
		this.playlist = playlistString != null ? FileStringListUtilities.parseFileStringList(playlistString) : new ArrayList<>();
		updatePreparedPlaybackFileProvider();
	}

	/* Begin playlist control */

	/**
	 * Seeks to the File with the file key in the playlist and beginning of the file.
	 * If a file in the playlist is already playing, it will begin playback.
	 * @param filePos The key of the file to seek to
	 */
	public void seekTo(int filePos) {
		seekTo(filePos, 0);
	}

	/**
	 * Seeks to the file key in the playlist and the start position in that file.
	 * If a file in the playlist is already playing, it will begin playback.
	 * @param filePos The key of the file to seek to
	 * @param fileProgress The position in the file to start at
	 */
	public void seekTo(final int filePos, final int fileProgress) throws IndexOutOfBoundsException {
		boolean wasPlaying = false;

		if (playbackHandler != null) {

			if (playbackHandler.isPlaying()) {

				// If the seek-to index is the same as that of the file playing, keep on playing
				if (filePos == currentFilePos) return;

				// stop any playback that is in action
				wasPlaying = true;
				playbackHandler.pause();
			}

			try {
				playbackHandler.close();
			} catch (IOException e) {
				mLogger.error("There was an error closing the playback handler", e);
			}
			playbackHandler = null;
		}

		if (filePos < 0) currentFilePos = 0;
        if (filePos >= playlist.size())
        	throw new IndexOutOfBoundsException("File position is greater than playlist size.");

        currentFilePos = filePos;

		updatePreparedPlaybackFileProvider();

		if (wasPlaying) {
			setupNextPreparedFile(fileProgress);
		}
	}

	/**
	 * Start playback of the playlist at the desired file key
	 * @param filePos The file key to start playback with
	 */
	public void startAt(int filePos) {
		startAt(filePos, 0);
	}

	/**
	 * Start playback of the playlist at the desired file key and at the desired position in the file
	 * @param filePos The file key to start playback with
	 * @param fileProgress The position in the file to start playback at
	 */
	public void startAt(int filePos, int fileProgress) {
		seekTo(filePos, fileProgress);
		setupNextPreparedFile();
	}

	public boolean resume() {
		if (playbackHandler == null) {
			setupNextPreparedFile();

			return true;
		}

		playbackHandler.start();
		return true;
	}

	private void startFilePlayback(IPlaybackHandler playbackHandler) {
		if (playbackHandler == null) {
			isPlaying = false;
			return;
		}

		currentFilePos++;

		isPlaying = true;

		this.playbackHandler = playbackHandler;

		playbackHandler.setVolume(mVolume);
		playbackHandler
			.start()
			.then(this::onFileComplete)
			.error(this::onFileError);

        // Throw events after asynchronous calls have started
        throwChangeEvent(mCurrentPlaybackFile);
        for (OnNowPlayingStartListener listener : mOnNowPlayingStartListeners)
        	listener.onNowPlayingStart(this, mCurrentPlaybackFile);
	}

	public void pause() {
		isPlaying = false;

		if (playbackHandler == null) return;

		if (playbackHandler.isPlaying()) playbackHandler.pause();
		for (OnNowPlayingPauseListener onPauseListener : mOnNowPlayingPauseListeners)
			onPauseListener.onNowPlayingPause(this, mCurrentPlaybackFile);
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public void setVolume(float volume) {
		mVolume = volume;
		if (mCurrentPlaybackFile != null && mCurrentPlaybackFile.isPlaying()) mCurrentPlaybackFile.setVolume(mVolume);
	}

	public void setIsRepeating(boolean isRepeating) {
		this.isRepeating = isRepeating;
		updatePreparedPlaybackFileProvider();
	}

	public boolean isRepeating() {
		return isRepeating;
	}

	/* End playlist control */

	public void addFile(final IFile file) {
		playlist.add(file);

		updatePreparedPlaybackFileProvider();
	}

	public void removeFile(final int position) {
		playlist.remove(position);

		updatePreparedPlaybackFileProvider();

		if (position != currentFilePos) return;

		playbackHandler.pause();

		setupNextPreparedFile();
	}

	public IPlaybackFile getCurrentPlaybackFile() {
		return mCurrentPlaybackFile;
	}

	public List<IFile> getPlaylist() {
		return Collections.unmodifiableList(playlist);
	}

	public String getPlaylistString() {
		return FileStringListUtilities.serializeFileStringList(playlist);
	}

	public int getCurrentPosition() {
		return currentFilePos;
	}

	private void onFileComplete(IPlaybackHandler playbackHandler) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			mLogger.error("There was an error releasing the media player", e);
		}

		setupNextPreparedFile();
	}

	private void updatePreparedPlaybackFileProvider() {
		final List<IFile> truncatedPlaylist = Stream.of(playlist).skip(preparingFileIndex - 1).collect(Collectors.toList());

		if (!isRepeating) {
			preparedPlaybackFileProvider =
				new QueuedMediaPlayerProvider(
					truncatedPlaylist,
					new BestMatchUriProvider(context, connectionProvider, library),
					new MediaPlayerInitializer(context, library));

			return;
		}

		final List<IFile> positionalPlaylist = new ArrayList<>(truncatedPlaylist);
		positionalPlaylist.addAll(playlist.subList(0, preparingFileIndex - 1));

		preparedPlaybackFileProvider =
			new CyclicalQueuedMediaPlayerProvider(
				positionalPlaylist,
				new BestMatchUriProvider(context, connectionProvider, library),
				new MediaPlayerInitializer(context, library));
	}

	private void setupNextPreparedFile() {
		setupNextPreparedFile(0);
	}

	private void setupNextPreparedFile(int preparedPosition) {
		preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		preparingPlaybackFile
			.then(handler -> {
				preparingFileIndex++;
				startFilePlayback(handler);
			})
			.error(this::onFileError);
	}

	private void onFileError(Exception exception) {
		if (!(exception instanceof MediaPlayerException)) {
			mLogger.error("There was an error preparing the file", exception);
			return;
		}

		final MediaPlayerException mediaPlayerException = (MediaPlayerException)exception;

		mLogger.error("JR File error - " + mediaPlayerException.what + " - " + mediaPlayerException.extra);

		// We don't know what happened, release the entire queue
		if (!PlaybackFile.MEDIA_ERROR_EXTRAS.contains(mediaPlayerException.extra))
			closePreparedPlaybackFileProvider();

		for (OnPlaylistStateControlErrorListener listener : mOnPlaylistStateControlErrorListeners)
			listener.onPlaylistStateControlError(this, mediaPlayerException);
	}
	/* End event handlers */

	/* Listener callers */
	private void throwChangeEvent(IPlaybackFile filePlayer) {
		for (OnNowPlayingChangeListener listener : mOnNowPlayingChangeListeners)
			listener.onNowPlayingChange(this, filePlayer);
	}

	private void throwStopEvent(IPlaybackFile filePlayer) {
		for (OnNowPlayingStopListener listener : mOnNowPlayingStopListeners)
			listener.onNowPlayingStop(this, filePlayer);
	}

	/* Listener collection helpers */
	public void addOnNowPlayingChangeListener(OnNowPlayingChangeListener listener) {
		mOnNowPlayingChangeListeners.add(listener);
	}

	public void removeOnNowPlayingChangeListener(OnNowPlayingChangeListener listener) {
		if (mOnNowPlayingChangeListeners.contains(listener))
			mOnNowPlayingChangeListeners.remove(listener);
	}

	public void addOnNowPlayingStartListener(OnNowPlayingStartListener listener) {
		mOnNowPlayingStartListeners.add(listener);
	}

	public void removeOnNowPlayingStartListener(OnNowPlayingStartListener listener) {
		if (mOnNowPlayingStartListeners.contains(listener))
			mOnNowPlayingStartListeners.remove(listener);
	}

	public void addOnNowPlayingStopListener(OnNowPlayingStopListener listener) {
		mOnNowPlayingStopListeners.add(listener);
	}

	public void removeOnNowPlayingStopListener(OnNowPlayingStopListener listener) {
		if (mOnNowPlayingStopListeners.contains(listener))
			mOnNowPlayingStopListeners.remove(listener);
	}

	public void addOnNowPlayingPauseListener(OnNowPlayingPauseListener listener) {
		mOnNowPlayingPauseListeners.add(listener);
	}

	public void removeOnNowPlayingPauseListener(OnNowPlayingPauseListener listener) {
		if (mOnNowPlayingPauseListeners.contains(listener))
			mOnNowPlayingPauseListeners.remove(listener);
	}

	public void addOnPlaylistStateControlErrorListener(OnPlaylistStateControlErrorListener listener) {
		mOnPlaylistStateControlErrorListeners.add(listener);
	}

	public void removeOnPlaylistStateControlErrorListener(OnPlaylistStateControlErrorListener listener) {
		if (mOnPlaylistStateControlErrorListeners.contains(listener))
			mOnPlaylistStateControlErrorListeners.remove(listener);
	}

	// Release all heavy resources
	public void release() {
		isPlaying = false;
		closePreparedPlaybackFileProvider();
	}

	private void closePreparedPlaybackFileProvider() {
		try {
			preparedPlaybackFileProvider.close();
		} catch (IOException e) {
			mLogger.error("There was an error closing the playback file provider", e);
		}
	}
}
