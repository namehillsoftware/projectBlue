package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.LoggerFactory;

import android.content.Context;

import com.lasthopesoftware.bluewater.BackgroundFilePreparer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingPauseListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.File;
import com.lasthopesoftware.bluewater.data.service.objects.Files;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnFilePreparedListener;

public class PlaylistController implements
	OnFilePreparedListener,
	OnFileErrorListener, 
	OnFileCompleteListener
{
	private HashSet<OnNowPlayingChangeListener> mOnNowPlayingChangeListeners = new HashSet<OnNowPlayingChangeListener>();
	private HashSet<OnNowPlayingStartListener> mOnNowPlayingStartListeners = new HashSet<OnNowPlayingStartListener>();
	private HashSet<OnNowPlayingStopListener> mOnNowPlayingStopListeners = new HashSet<OnNowPlayingStopListener>();
	private HashSet<OnNowPlayingPauseListener> mOnNowPlayingPauseListeners = new HashSet<OnNowPlayingPauseListener>();
	private HashSet<OnPlaylistStateControlErrorListener> mOnPlaylistStateControlErrorListeners = new HashSet<OnPlaylistStateControlErrorListener>();
	private ArrayList<File> mPlaylist;
	private int mFileKey = -1;
	private FilePlayer mCurrentFilePlayer, mNextFilePlayer;
	private Context mContext;
	private BackgroundFilePreparer mBackgroundFilePreparerTask;
	private float mVolume = 1.0f;
	private boolean mIsRepeating = false;
	
	public PlaylistController(Context context, String playlistString) {
		this(context, playlistString != null ? Files.deserializeFileStringList(playlistString) : new ArrayList<File>());
	}
	
	public PlaylistController(Context context, ArrayList<File> playlist) {
		mContext = context;
		mPlaylist = playlist;
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
			// If the track is already playing, keep on playing
			if (mPlaylist.indexOf(mCurrentFilePlayer.getFile()) == filePos && mCurrentFilePlayer.isMediaPlayerCreated()) return;
			
			// stop any playback that is in action
			if (mCurrentFilePlayer.isPlaying()) {
				wasPlaying = true;
				mCurrentFilePlayer.stop();
			}
			
			mCurrentFilePlayer.releaseMediaPlayer();
			mCurrentFilePlayer = null;
		}
		
		if (filePos < 0) filePos = 0;
        if (filePos >= mPlaylist.size())
        	throw new IndexOutOfBoundsException("File position is greater than playlist size.");
		
		final File file = mPlaylist.get(filePos);
		final FilePlayer filePlayer = new FilePlayer(mContext, file);
		filePlayer.addOnFileCompleteListener(this);
		filePlayer.addOnFilePreparedListener(this);
		filePlayer.addOnFileErrorListener(this);
		filePlayer.initMediaPlayer();
		filePlayer.seekTo(fileProgress < 0 ? filePlayer.getCurrentPosition() : fileProgress);
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
		
		if (!mCurrentFilePlayer.isMediaPlayerCreated())
			mCurrentFilePlayer.initMediaPlayer();
		
		if (!mCurrentFilePlayer.isPrepared()) {
			mCurrentFilePlayer.prepareMediaPlayer();
			return true;
		}
		
		startFilePlayback(mCurrentFilePlayer);
		return true;
	}

	private void startFilePlayback(FilePlayer mediaPlayer) {
		mCurrentFilePlayer = mediaPlayer;
		
		mediaPlayer.setVolume(mVolume);
		mediaPlayer.start();
		
		mFileKey = mediaPlayer.getFile().getKey();
		
		haltBackgroundPreparerThread();
		
		File nextFile = mediaPlayer.getFile().getNextFile();
		if (nextFile == null) {
			if (!mIsRepeating) {
				if (mNextFilePlayer != null && mNextFilePlayer != mCurrentFilePlayer) mNextFilePlayer.releaseMediaPlayer();
				return;
			} else {
				nextFile = mPlaylist.get(0);
			}
		}
		
        if (nextFile != null)
        	prepareNextFile(nextFile);
        
        // Throw events after asynchronous calls have started
        throwChangeEvent(mCurrentFilePlayer);
        for (OnNowPlayingStartListener listener : mOnNowPlayingStartListeners)
        	listener.onNowPlayingStart(this, mCurrentFilePlayer);
	}
	
	private void prepareNextFile(File nextFile) {
		mNextFilePlayer = new FilePlayer(mContext, nextFile);
		
		haltBackgroundPreparerThread();
    	
    	mBackgroundFilePreparerTask = new BackgroundFilePreparer(mCurrentFilePlayer, mNextFilePlayer);;
    	mBackgroundFilePreparerTask.start();    	
	}
	
	public void pause() {
		haltBackgroundPreparerThread();
		if (mCurrentFilePlayer == null) return;
		
		if (mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.pause();
		for (OnNowPlayingPauseListener onPauseListener : mOnNowPlayingPauseListeners)
			onPauseListener.onNowPlayingPause(this, mCurrentFilePlayer);
	}
	
	public boolean isPrepared() {
		return mCurrentFilePlayer != null && mCurrentFilePlayer.isPrepared();
	}
	
	public boolean isPlaying() {
		return mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying();
	}
	
	public void setVolume(float volume) {
		mVolume = volume;
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.setVolume(mVolume);
	}
	
	public void setIsRepeating(boolean isRepeating) {
		mIsRepeating = isRepeating;
		
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying() && mCurrentFilePlayer.getFile().getNextFile() == null) {
			if (mIsRepeating) {
				mPlaylist.get(mPlaylist.size() - 1).setNextFile(mPlaylist.get(0));
				prepareNextFile(mPlaylist.get(0));
			} else {
				haltBackgroundPreparerThread();
				if (mPlaylist.get(mPlaylist.size() - 1).getNextFile() != null) mPlaylist.get(mPlaylist.size() - 1).setNextFile(null);
				if (mNextFilePlayer != null) mNextFilePlayer.releaseMediaPlayer();
				mNextFilePlayer = null;
			}
		}
	}
	
	public boolean isRepeating() {
		return mIsRepeating;
	}
	
	/* End playlist control */
	
	public void addFile(int fileKey) {
		addFile(new File(fileKey));
	}
	
	public void addFile(File file) {
		file.setPreviousFile(mPlaylist.get(mPlaylist.size() - 1));
		mPlaylist.add(file);
	}
	
	public FilePlayer getCurrentFilePlayer() {
		return mCurrentFilePlayer;
	}
	
	public List<File> getPlaylist() {
		return Collections.unmodifiableList(mPlaylist);
	}
	
	public int getCurrentPosition() {
		return mCurrentFilePlayer != null ? mPlaylist.indexOf(mCurrentFilePlayer.getFile()) : 0;
	}

	/* Event handlers */
	@Override
	public void onJrFilePrepared(FilePlayer mediaPlayer) {
		if (mediaPlayer.isPlaying()) return;
		
		startFilePlayback(mediaPlayer);
	}
	
	@Override
	public void onJrFileComplete(FilePlayer mediaPlayer) {
		mediaPlayer.releaseMediaPlayer();
		
		if (mNextFilePlayer == null) {
			if (mediaPlayer.getFile().getNextFile() == null) {
				throwStopEvent(mediaPlayer);
				return;
			}
			
			mNextFilePlayer = new FilePlayer(mContext, mediaPlayer.getFile().getNextFile());
		}
		
		mNextFilePlayer.addOnFileCompleteListener(this);
		mNextFilePlayer.addOnFileErrorListener(this);
		if (!mNextFilePlayer.isPrepared()) {
			mNextFilePlayer.addOnFilePreparedListener(this);
			mNextFilePlayer.prepareMediaPlayer();
			return;
		}
		
		startFilePlayback(mNextFilePlayer);
	}
	
	@Override
	public boolean onJrFileError(FilePlayer mediaPlayer, int what, int extra) {
		LoggerFactory.getLogger(PlaylistController.class).error("JR File error - " + what + " - " + extra);
		pause();
		
		for (OnPlaylistStateControlErrorListener listener : mOnPlaylistStateControlErrorListeners) {
			if (listener.onPlaylistStateControlError(this, mediaPlayer)) return true;
		}
		
		return false;
	}
	/* End event handlers */
	
	/* Listener callers */
	private void throwChangeEvent(FilePlayer filePlayer) {
		for (OnNowPlayingChangeListener listener : mOnNowPlayingChangeListeners)
			listener.onNowPlayingChange(this, filePlayer);
	}
	
	private void throwStopEvent(FilePlayer filePlayer) {
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
		if (mCurrentFilePlayer != null) mCurrentFilePlayer.releaseMediaPlayer();
		if (mNextFilePlayer != null) mNextFilePlayer.releaseMediaPlayer();
		
		haltBackgroundPreparerThread();
	}
	
	private void haltBackgroundPreparerThread() {
		if (mBackgroundFilePreparerTask != null && !mBackgroundFilePreparerTask.isDone()) mBackgroundFilePreparerTask.cancel();
	}
}
