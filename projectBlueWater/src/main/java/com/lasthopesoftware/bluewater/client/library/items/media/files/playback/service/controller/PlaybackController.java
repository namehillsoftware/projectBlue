package com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.controller;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;

import com.lasthopesoftware.bluewater.client.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.access.stringlist.FileStringListUtilities;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.IPlaybackHandler;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.error.MediaPlayerException;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPreparedPlaybackFileProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IPreparedPlayerStateTracker;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.IProvidePlaybackQueues;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.file.preparation.PreparedStateTrackingPlayerProvider;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.client.library.items.media.files.playback.service.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.promises.IPromise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaybackController {
	@SuppressLint("InlinedApi")
	public static final Set<Integer> MEDIA_ERROR_EXTRAS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new Integer[]{
		MediaPlayer.MEDIA_ERROR_IO,
		MediaPlayer.MEDIA_ERROR_MALFORMED,
		MediaPlayer.MEDIA_ERROR_UNSUPPORTED,
		MediaPlayer.MEDIA_ERROR_TIMED_OUT,
		MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
	})));

	private final HashSet<OnNowPlayingChangeListener> mOnNowPlayingChangeListeners = new HashSet<>();
	private final HashSet<OnNowPlayingStartListener> mOnNowPlayingStartListeners = new HashSet<>();
	private final HashSet<OnNowPlayingStopListener> mOnNowPlayingStopListeners = new HashSet<>();
	private final HashSet<OnNowPlayingPauseListener> mOnNowPlayingPauseListeners = new HashSet<>();
	private final HashSet<OnPlaylistStateControlErrorListener> mOnPlaylistStateControlErrorListeners = new HashSet<>();

	private final ArrayList<IFile> playlist;
	private final IProvidePlaybackQueues playbackQueuesProvider;

	private float volume = 1.0f;
	private boolean isRepeating = false;
	private boolean isPlaying = false;

	private static final Logger logger = LoggerFactory.getLogger(PlaybackController.class);

	private IPreparedPlayerStateTracker preparedPlaybackFileProvider;
	private IPlaybackHandler playbackHandler;
	private int currentFilePos;

	public PlaybackController(final String playlistString, IProvidePlaybackQueues playbackQueuesProvider) {
		this.playlist = playlistString != null ? FileStringListUtilities.parseFileStringList(playlistString) : new ArrayList<>();

		this.playbackQueuesProvider = playbackQueuesProvider;
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
		if (filePos >= playlist.size())
			throw new IndexOutOfBoundsException("File position is greater than playlist size.");

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
				logger.error("There was an error closing the playback handler", e);
			}

			playbackHandler = null;
		}

		currentFilePos = Math.min(filePos, 0);
		updatePreparedPlaybackFileProvider(currentFilePos);

		if (wasPlaying)
			setupNextPreparedFile(fileProgress);
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

		playbackHandler.setVolume(volume);
		playbackHandler
			.start()
			.then(this::closeAndStartNextFile)
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
		this.volume = volume;
		if (playbackHandler != null && playbackHandler.isPlaying()) playbackHandler.setVolume(this.volume);
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

		if (playbackHandler == null || !playbackHandler.isPlaying())
			return;

		playbackHandler.pause();
		closeAndStartNextFile(playbackHandler);
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

	private void closeAndStartNextFile(IPlaybackHandler playbackHandler) {
		try {
			playbackHandler.close();
		} catch (IOException e) {
			logger.error("There was an error releasing the media player", e);
		}

		setupNextPreparedFile();
	}

	private void updatePreparedPlaybackFileProvider() {
		updatePreparedPlaybackFileProvider(preparedPlaybackFileProvider.getPreparedIndex());
	}

	private void updatePreparedPlaybackFileProvider(int newPosition) {
		closePreparedPlaybackFileProvider();

		final IPreparedPlaybackFileProvider newPlaybackFileProvider	=
			playbackQueuesProvider.getQueue(playlist, newPosition, isRepeating);

		preparedPlaybackFileProvider = new PreparedStateTrackingPlayerProvider(newPosition, playlist.size(), newPlaybackFileProvider);
	}

	private void setupNextPreparedFile() {
		setupNextPreparedFile(0);
	}

	private void setupNextPreparedFile(int preparedPosition) {
		final IPromise<IPlaybackHandler> preparingPlaybackFile =
			preparedPlaybackFileProvider
				.promiseNextPreparedPlaybackFile(preparedPosition);

		preparingPlaybackFile
			.then(this::startFilePlayback)
			.error(this::onFileError);
	}

	private void onFileError(Exception exception) {
		if (!(exception instanceof MediaPlayerException)) {
			logger.error("There was an error preparing the file", exception);
			return;
		}

		final MediaPlayerException mediaPlayerException = (MediaPlayerException)exception;

		logger.error("JR File error - " + mediaPlayerException.what + " - " + mediaPlayerException.extra);

		// We don't know what happened, release the entire queue
		if (!MEDIA_ERROR_EXTRAS.contains(mediaPlayerException.extra))
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
			logger.error("There was an error closing the playback file provider", e);
		}
	}
}
