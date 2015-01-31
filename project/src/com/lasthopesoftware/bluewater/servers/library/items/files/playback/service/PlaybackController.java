package com.lasthopesoftware.bluewater.servers.library.items.files.playback.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.IPlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.IPlaybackFileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.PlaybackFile;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.file.PlaybackFileProvider;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFileBufferedListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFileErrorListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnFilePreparedListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.servers.library.items.files.playback.listeners.OnPlaylistStateControlErrorListener;

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
	private String mPlaylistString;
	private final IPlaybackFileProvider mPlaybackFileProvider;
	private int mFileKey = -1;
	private int mCurrentFilePos;
	private IPlaybackFile mCurrentFilePlayer, mNextFilePlayer;
	private float mVolume = 1.0f;
	private boolean mIsRepeating = false;
	private boolean mIsPlaying = false;
	
	private static final Logger mLogger = LoggerFactory.getLogger(PlaybackController.class);
	
	public PlaybackController(final Context context, final String playlistString) {
		this(context, playlistString != null ? Files.deserializeFileStringList(playlistString) : new ArrayList<File>());
		
		mPlaylistString = playlistString;
	}
	
	public PlaybackController(final Context context, final ArrayList<File> playlist) {
		mPlaybackFileProvider = new PlaybackFileProvider(context, playlist);
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
		
		if (mCurrentFilePlayer != null) {
			
			if (mCurrentFilePlayer.isPlaying()) {
				
				// If the seek-to index is the same as that of the file playing, keep on playing
				if (filePos == mCurrentFilePos) return;
			
				// stop any playback that is in action
				wasPlaying = true;
				mCurrentFilePlayer.stop();
			}
			
			mCurrentFilePlayer.releaseMediaPlayer();
			mCurrentFilePlayer = null;
		}
		
		if (filePos < 0) filePos = 0;
        if (filePos >= mPlaybackFileProvider.size())
        	throw new IndexOutOfBoundsException("File position is greater than playlist size.");
		
        mCurrentFilePos = filePos;
        
		final IPlaybackFile filePlayer = mPlaybackFileProvider.getPlaybackFile(mCurrentFilePos);
		filePlayer.addOnFileCompleteListener(this);
		filePlayer.addOnFilePreparedListener(this);
		filePlayer.addOnFileErrorListener(this);
		filePlayer.initMediaPlayer();
		filePlayer.seekTo(fileProgress < 0 ? 0 : fileProgress);
		mCurrentFilePlayer = filePlayer;
		if (wasPlaying) mCurrentFilePlayer.prepareMediaPlayer();
		throwChangeEvent(mCurrentFilePlayer);
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
		if (mCurrentFilePlayer == null || mCurrentFilePlayer.isPlaying()) return;
		if (!mCurrentFilePlayer.isPrepared()) mCurrentFilePlayer.prepareMediaPlayer(); // prepare async to not block main thread
		else startFilePlayback(mCurrentFilePlayer);
	}
	
	public boolean resume() {
		if (mCurrentFilePlayer == null) {
			if (mFileKey == -1) return false;
			
			startAt(mFileKey);
			return true;
		}
		
		if (!mCurrentFilePlayer.isMediaPlayerCreated()) {
			mCurrentFilePlayer.addOnFileCompleteListener(this);
			mCurrentFilePlayer.addOnFilePreparedListener(this);
			mCurrentFilePlayer.addOnFileErrorListener(this);
			
			mCurrentFilePlayer.initMediaPlayer();
		}
		
		if (!mCurrentFilePlayer.isPrepared()) {
			mCurrentFilePlayer.prepareMediaPlayer();
			return true;
		}
		
		startFilePlayback(mCurrentFilePlayer);
		return true;
	}

	private void startFilePlayback(IPlaybackFile playbackFile) {
		mIsPlaying = true;
		mCurrentFilePlayer = playbackFile;
		playbackFile.setVolume(mVolume);
		playbackFile.start();
		
		mFileKey = playbackFile.getFile().getKey();
		
		int nextFileIndex = mCurrentFilePos + 1;
		if (nextFileIndex >= mPlaybackFileProvider.size()) {
			if (!mIsRepeating) {
				if (mNextFilePlayer != null && mNextFilePlayer != mCurrentFilePlayer) mNextFilePlayer.releaseMediaPlayer();
				mNextFilePlayer = null;
				nextFileIndex = -1;
			} else {
				nextFileIndex = 0;
			}
		}
		
        if (nextFileIndex > -1)
        	prepareNextFile(nextFileIndex);
        
        // Throw events after asynchronous calls have started
        throwChangeEvent(mCurrentFilePlayer);
        for (OnNowPlayingStartListener listener : mOnNowPlayingStartListeners)
        	listener.onNowPlayingStart(this, mCurrentFilePlayer);
	}
	
	private void prepareNextFile(final int filePos) {		
		if (mCurrentFilePlayer == null) return;
		
		if (mNextFilePlayer == null || mNextFilePlayer.getFile() != mPlaybackFileProvider.getPlaybackFile(filePos).getFile()) {
			if (mNextFilePlayer != null && mNextFilePlayer != mCurrentFilePlayer)
				mNextFilePlayer.releaseMediaPlayer();
			
			mNextFilePlayer = mPlaybackFileProvider.getPlaybackFile(filePos);
		}
				
		if (mNextFilePlayer.isPrepared()) return;
		
		if (mCurrentFilePlayer.isBuffered())
			onFileBuffered(mCurrentFilePlayer);
		else
			mCurrentFilePlayer.addOnFileBufferedListener(this);
	}

	@Override
	public void onFileBuffered(IPlaybackFile filePlayer) {
		mNextFilePlayer.initMediaPlayer();
		mNextFilePlayer.prepareMediaPlayer();
	}
	
	public void pause() {
		mIsPlaying = false;

		if (mCurrentFilePlayer == null) return;
		
		if (mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.pause();
		for (OnNowPlayingPauseListener onPauseListener : mOnNowPlayingPauseListeners)
			onPauseListener.onNowPlayingPause(this, mCurrentFilePlayer);
	}
	
	public boolean isPrepared() {
		return mCurrentFilePlayer != null && mCurrentFilePlayer.isPrepared();
	}
	
	public boolean isPlaying() {
		return mIsPlaying;
	}
	
	public void setVolume(float volume) {
		mVolume = volume;
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.setVolume(mVolume);
	}
	
	public void setIsRepeating(boolean isRepeating) {
		mIsRepeating = isRepeating;
		
		if (mCurrentFilePlayer == null) return;
		
		final File lastFile = mPlaybackFileProvider.lastPlaybackFile().getFile();
		if (mIsRepeating) {
			lastFile.setNextFile(mPlaybackFileProvider.getPlaybackFile(0).getFile());
			if (lastFile == mCurrentFilePlayer.getFile()) {
				if (mNextFilePlayer != null) mNextFilePlayer.releaseMediaPlayer();
				prepareNextFile(0);
			}
			return;
		}
		
		if (lastFile.getNextFile() != null) lastFile.setNextFile(null);
		
		if (lastFile == mCurrentFilePlayer.getFile()) {
			if (mNextFilePlayer != null) mNextFilePlayer.releaseMediaPlayer();
			mNextFilePlayer = null;
		}
	}
	
	public boolean isRepeating() {
		return mIsRepeating;
	}
	
	/* End playlist control */
	
	public void addFile(final int fileKey) {
		addFile(new File(fileKey));
	}
	
	public void addFile(final File file) {
		file.setPreviousFile(mPlaybackFileProvider.getPlaybackFile(mPlaybackFileProvider.size() - 1).getFile());
		mPlaybackFileProvider.add(file);
	}
	
	public void removeFile(final int position) {
		final File file = mPlaybackFileProvider.getPlaybackFile(position).getFile();
		mPlaybackFileProvider.remove(position);
		
		final File nextFile = file.getNextFile();
		final File previousFile = file.getPreviousFile();
		
		if (previousFile != null)
			previousFile.setNextFile(nextFile);
		
		if (nextFile != null)
			nextFile.setPreviousFile(previousFile);
		
		if (position != mCurrentFilePos) return;
		
		if (nextFile != null) {
			seekTo(mCurrentFilePos + 1);
			return;
		}
		
		mCurrentFilePlayer.stop();
		if (previousFile != null)
			seekTo(mCurrentFilePos - 1);
	}
	
	public IPlaybackFile getCurrentFilePlayer() {
		return mCurrentFilePlayer;
	}
	
	public List<File> getPlaylist() {
		return mPlaybackFileProvider.getFiles();
	}
	
	public String getPlaylistString() {
		if (mPlaylistString == null)
			mPlaylistString = Files.serializeFileStringList(getPlaylist());
		
		return mPlaylistString;
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
		
		if (mNextFilePlayer == null) {
			final int nextFilePos = mCurrentFilePos + 1;
			// Playlist is complete, throw stop event and get out
			if (!mIsRepeating && nextFilePos >= mPlaybackFileProvider.size()) {
				mIsPlaying = false;
				throwStopEvent(mediaPlayer);
				return;
			}
			
			mNextFilePlayer = mPlaybackFileProvider.getPlaybackFile(nextFilePos);
		}
		
		// Move the pointer early so that getting the currently playing file is correctly
		// returned
		mCurrentFilePos++;
		mCurrentFilePlayer = mNextFilePlayer;
		mCurrentFilePlayer.addOnFileCompleteListener(this);
		mCurrentFilePlayer.addOnFileErrorListener(this);
		if (!mCurrentFilePlayer.isPrepared()) {
			mLogger.warn("File " + mCurrentFilePlayer.getFile().getValue() + " was not prepared. Preparing now.");
			if (!mCurrentFilePlayer.isMediaPlayerCreated())
				mCurrentFilePlayer.initMediaPlayer();
			
			mCurrentFilePlayer.addOnFilePreparedListener(this);
			mCurrentFilePlayer.prepareMediaPlayer();
			return;
		}
		
		startFilePlayback(mCurrentFilePlayer);
	}
	
	@Override
	public void onFileError(IPlaybackFile mediaPlayer, int what, int extra) {
		mLogger.error("JR File error - " + what + " - " + extra);
		
		// We don't know what happened, release the next file player too
		if (!PlaybackFile.MEDIA_ERROR_EXTRAS.contains(extra) && mNextFilePlayer != null && mediaPlayer != mNextFilePlayer)
			mNextFilePlayer.releaseMediaPlayer();
		
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
		if (mCurrentFilePlayer != null) mCurrentFilePlayer.releaseMediaPlayer();
		if (mNextFilePlayer != null) mNextFilePlayer.releaseMediaPlayer();
	}
}
