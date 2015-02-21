package com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.File;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.Files;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.IFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.IPlaybackFileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.PlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.PlaybackFileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.file.listeners.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.playback.service.listeners.OnPlaylistStateControlErrorListener;

public class PlaybackController implements
	OnFilePreparedListener,
	OnFileErrorListener, 
	OnFileCompleteListener,
	OnFileBufferedListener
{
	private final HashSet<OnNowPlayingChangeListener> mOnNowPlayingChangeListeners = new HashSet<OnNowPlayingChangeListener>();
	private final HashSet<OnNowPlayingStartListener> mOnNowPlayingStartListeners = new HashSet<OnNowPlayingStartListener>();
	private final HashSet<OnNowPlayingStopListener> mOnNowPlayingStopListeners = new HashSet<OnNowPlayingStopListener>();
	private final HashSet<OnNowPlayingPauseListener> mOnNowPlayingPauseListeners = new HashSet<OnNowPlayingPauseListener>();
	private final HashSet<OnPlaylistStateControlErrorListener> mOnPlaylistStateControlErrorListeners = new HashSet<OnPlaylistStateControlErrorListener>();
	
	private final IPlaybackFileProvider mPlaybackFileProvider;
	private int mFileKey = -1;
	private int mCurrentFilePos;
	private IPlaybackFile mCurrentPlaybackFile, mNextPlaybackFile;
	
	private float mVolume = 1.0f;
	private boolean mIsRepeating = false;
	private boolean mIsPlaying = false;
	
	private static final Logger mLogger = LoggerFactory.getLogger(PlaybackController.class);
	
	public PlaybackController(final Context context, final String playlistString) {
		this(context, playlistString != null ? Files.parseFileStringList(playlistString) : new ArrayList<IFile>());
	}
	
	public PlaybackController(final Context context, final ArrayList<IFile> playlist) {
		this(new PlaybackFileProvider(context, playlist));
	}
	
	public PlaybackController(IPlaybackFileProvider playbackFileProvider) {
		mPlaybackFileProvider = playbackFileProvider;
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
	public void seekTo(int filePos, int fileProgress) throws IndexOutOfBoundsException {
		boolean wasPlaying = false;
		
		if (mCurrentPlaybackFile != null) {
			
			if (mCurrentPlaybackFile.isPlaying()) {
				
				// If the seek-to index is the same as that of the file playing, keep on playing
				if (filePos == mCurrentFilePos) return;
			
				// stop any playback that is in action
				wasPlaying = true;
				mCurrentPlaybackFile.stop();
			}
			
			mCurrentPlaybackFile.releaseMediaPlayer();
			mCurrentPlaybackFile = null;
		}
		
		if (filePos < 0) filePos = 0;
        if (filePos >= mPlaybackFileProvider.size())
        	throw new IndexOutOfBoundsException("File position is greater than playlist size.");
		
        mCurrentFilePos = filePos;
        
		final IPlaybackFile filePlayer = mPlaybackFileProvider.getNewPlaybackFile(mCurrentFilePos);
		filePlayer.addOnFileCompleteListener(this);
		filePlayer.addOnFilePreparedListener(this);
		filePlayer.addOnFileErrorListener(this);
		filePlayer.initMediaPlayer();
		filePlayer.seekTo(fileProgress < 0 ? 0 : fileProgress);
		mCurrentPlaybackFile = filePlayer;
		if (wasPlaying) mCurrentPlaybackFile.prepareMediaPlayer();
		throwChangeEvent(mCurrentPlaybackFile);
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
		if (mCurrentPlaybackFile == null || mCurrentPlaybackFile.isPlaying()) return;
		if (!mCurrentPlaybackFile.isPrepared()) mCurrentPlaybackFile.prepareMediaPlayer(); // prepare async to not block main thread
		else startFilePlayback(mCurrentPlaybackFile);
	}
	
	public boolean resume() {
		if (mCurrentPlaybackFile == null) {
			if (mFileKey == -1) return false;
			
			startAt(mFileKey);
			return true;
		}
		
		if (!mCurrentPlaybackFile.isMediaPlayerCreated()) {
			mCurrentPlaybackFile.addOnFileCompleteListener(this);
			mCurrentPlaybackFile.addOnFilePreparedListener(this);
			mCurrentPlaybackFile.addOnFileErrorListener(this);
			
			mCurrentPlaybackFile.initMediaPlayer();
		}
		
		if (!mCurrentPlaybackFile.isPrepared()) {
			mCurrentPlaybackFile.prepareMediaPlayer();
			return true;
		}
		
		startFilePlayback(mCurrentPlaybackFile);
		return true;
	}

	private void startFilePlayback(IPlaybackFile playbackFile) {
		mIsPlaying = true;
		mCurrentPlaybackFile = playbackFile;
		playbackFile.setVolume(mVolume);
		playbackFile.start();
		
		mFileKey = playbackFile.getFile().getKey();
		
		int nextFileIndex = mCurrentFilePos + 1;
		if (nextFileIndex >= mPlaybackFileProvider.size()) {
			if (!mIsRepeating) {
				if (mNextPlaybackFile != null && mNextPlaybackFile != mCurrentPlaybackFile) mNextPlaybackFile.releaseMediaPlayer();
				mNextPlaybackFile = null;
				nextFileIndex = -1;
			} else {
				nextFileIndex = 0;
			}
		}
		
        if (nextFileIndex > -1)
        	prepareNextFile(nextFileIndex);
        
        // Throw events after asynchronous calls have started
        throwChangeEvent(mCurrentPlaybackFile);
        for (OnNowPlayingStartListener listener : mOnNowPlayingStartListeners)
        	listener.onNowPlayingStart(this, mCurrentPlaybackFile);
	}
	
	private void prepareNextFile(final int filePos) {		
		if (mCurrentPlaybackFile == null) return;
		
		if (mNextPlaybackFile == null || mNextPlaybackFile.getFile() != mPlaybackFileProvider.getNewPlaybackFile(filePos).getFile()) {
			if (mNextPlaybackFile != null && mNextPlaybackFile != mCurrentPlaybackFile)
				mNextPlaybackFile.releaseMediaPlayer();
			
			mNextPlaybackFile = mPlaybackFileProvider.getNewPlaybackFile(filePos);
		}
				
		if (mNextPlaybackFile.isPrepared()) return;
		
		if (mCurrentPlaybackFile.isBuffered())
			onFileBuffered(mCurrentPlaybackFile);
		else
			mCurrentPlaybackFile.addOnFileBufferedListener(this);
	}

	@Override
	public void onFileBuffered(IPlaybackFile filePlayer) {
		mNextPlaybackFile.initMediaPlayer();
		mNextPlaybackFile.prepareMediaPlayer();
	}
	
	public void pause() {
		mIsPlaying = false;

		if (mCurrentPlaybackFile == null) return;
		
		if (mCurrentPlaybackFile.isPlaying()) mCurrentPlaybackFile.pause();
		for (OnNowPlayingPauseListener onPauseListener : mOnNowPlayingPauseListeners)
			onPauseListener.onNowPlayingPause(this, mCurrentPlaybackFile);
	}
	
	public boolean isPrepared() {
		return mCurrentPlaybackFile != null && mCurrentPlaybackFile.isPrepared();
	}
	
	public boolean isPlaying() {
		return mIsPlaying;
	}
	
	public void setVolume(float volume) {
		mVolume = volume;
		if (mCurrentPlaybackFile != null && mCurrentPlaybackFile.isPlaying()) mCurrentPlaybackFile.setVolume(mVolume);
	}
	
	public void setIsRepeating(boolean isRepeating) {
		mIsRepeating = isRepeating;
		
		if (mCurrentPlaybackFile == null) return;
		
		final IFile lastFile = mPlaybackFileProvider.getFiles().get(mPlaybackFileProvider.size() - 1);
				
		if (lastFile == mCurrentPlaybackFile.getFile()) {
			if (mNextPlaybackFile != null) mNextPlaybackFile.releaseMediaPlayer();
			
			if (mIsRepeating) prepareNextFile(0);
			else mNextPlaybackFile = null;
		}
	}
	
	public boolean isRepeating() {
		return mIsRepeating;
	}
	
	/* End playlist control */
	
	public void addFile(final int fileKey) {
		addFile(new File(fileKey));
	}
	
	public void addFile(final IFile file) {
		mPlaybackFileProvider.add(file);
	}
	
	public void removeFile(final int position) {
		mPlaybackFileProvider.remove(position);
		
		if (position != mCurrentFilePos) return;
		
		mCurrentPlaybackFile.stop();
		
		// First try seeking to the next file
		if (position < mPlaybackFileProvider.size()) {
			seekTo(position);
			return;
		}
		// If the next file is greater than the size, seek to the previous file if that's possible
		if (position > 0)
			seekTo(position - 1);
	}
	
	public IPlaybackFile getCurrentPlaybackFile() {
		return mCurrentPlaybackFile;
	}
	
	public List<IFile> getPlaylist() {
		return Collections.unmodifiableList(mPlaybackFileProvider.getFiles());
	}
	
	public String getPlaylistString() {
		return mPlaybackFileProvider.toPlaylistString();
	}
	
	public int getCurrentPosition() {
		return mCurrentFilePos;
	}

	/* Event handlers */
	@Override
	public void onFilePrepared(IPlaybackFile mediaPlayer) {
		if (mediaPlayer.isPlaying()) return;
		
		startFilePlayback(mediaPlayer);
	}
	
	@Override
	public void onFileComplete(IPlaybackFile mediaPlayer) {
		mediaPlayer.releaseMediaPlayer();
		
		final boolean isLastFile = mCurrentFilePos == mPlaybackFileProvider.size() - 1;
		// store the next file position in a local variable in case it is decided to not set
		// the current file position (such as in the not repeat scenario below)
		final int nextFilePos = !isLastFile ? mCurrentFilePos + 1 : 0;
		
		if (mNextPlaybackFile == null) {
			// Playlist is complete, throw stop event and get out
			if (!mIsRepeating && isLastFile) {
				mIsPlaying = false;
				throwStopEvent(mediaPlayer);
				return;
			}
			
			mNextPlaybackFile = mPlaybackFileProvider.getNewPlaybackFile(nextFilePos);
		}
		
		// Move the pointer early so that getting the currently playing file is correctly
		// returned
		mCurrentFilePos = nextFilePos;
		mCurrentPlaybackFile = mNextPlaybackFile;
		mCurrentPlaybackFile.addOnFileCompleteListener(this);
		mCurrentPlaybackFile.addOnFileErrorListener(this);
		if (!mCurrentPlaybackFile.isPrepared()) {
			mLogger.warn("File " + mCurrentPlaybackFile.getFile().getValue() + " was not prepared. Preparing now.");
			if (!mCurrentPlaybackFile.isMediaPlayerCreated())
				mCurrentPlaybackFile.initMediaPlayer();
			
			mCurrentPlaybackFile.addOnFilePreparedListener(this);
			mCurrentPlaybackFile.prepareMediaPlayer();
			return;
		}
		
		startFilePlayback(mCurrentPlaybackFile);
	}
	
	@Override
	public void onFileError(IPlaybackFile mediaPlayer, int what, int extra) {
		mLogger.error("JR File error - " + what + " - " + extra);
		
		// We don't know what happened, release the next file player too
		if (!PlaybackFile.MEDIA_ERROR_EXTRAS.contains(extra) && mNextPlaybackFile != null && mediaPlayer != mNextPlaybackFile)
			mNextPlaybackFile.releaseMediaPlayer();
		
		for (OnPlaylistStateControlErrorListener listener : mOnPlaylistStateControlErrorListeners)
			listener.onPlaylistStateControlError(this, mediaPlayer);
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
		mIsPlaying = false;
		if (mCurrentPlaybackFile != null) mCurrentPlaybackFile.releaseMediaPlayer();
		if (mNextPlaybackFile != null) mNextPlaybackFile.releaseMediaPlayer();
	}
}
