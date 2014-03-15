package com.lasthopesoftware.bluewater.data.service.helpers.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.LoggerFactory;

import android.content.Context;

import com.lasthopesoftware.bluewater.BackgroundFilePreparer;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingChangeListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStartListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnNowPlayingStopListener;
import com.lasthopesoftware.bluewater.data.service.helpers.playback.listeners.OnPlaylistStateControlErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.JrFile;
import com.lasthopesoftware.bluewater.data.service.objects.JrFiles;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileCompleteListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFileErrorListener;
import com.lasthopesoftware.bluewater.data.service.objects.OnJrFilePreparedListener;

public class JrPlaylistController implements
	OnJrFilePreparedListener,
	OnJrFileErrorListener, 
	OnJrFileCompleteListener
{
	private HashSet<OnNowPlayingChangeListener> mOnNowPlayingChangeListeners = new HashSet<OnNowPlayingChangeListener>();
	private HashSet<OnNowPlayingStartListener> mOnNowPlayingStartListeners = new HashSet<OnNowPlayingStartListener>();
	private HashSet<OnNowPlayingStopListener> mOnNowPlayingStopListeners = new HashSet<OnNowPlayingStopListener>();
	private HashSet<OnPlaylistStateControlErrorListener> mOnPlaylistStateControlErrorListeners = new HashSet<OnPlaylistStateControlErrorListener>();
	private ArrayList<JrFile> mPlaylist;
	private int mFileKey = -1;
	private JrFilePlayer mCurrentFilePlayer, mNextFilePlayer;
	private Context mContext;
	private Thread mBackgroundFilePreparerThread;
	private float mVolume = 1.0f;
	private boolean mIsRepeating = false;
	
	public JrPlaylistController(Context context, String playlistString) {
		this(context, JrFiles.deserializeFileStringList(playlistString));
	}
	
	public JrPlaylistController(Context context, ArrayList<JrFile> playlist) {
		mContext = context;
		mPlaylist = playlist;
	}
	
	/* Begin playlist control */
	
	public void seekTo(int fileKey) {
		seekTo(fileKey, 0);
	}
	
	public void seekTo(int fileKey, int startPos) {
		// If the track is already playing, keep on playing
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.getFile().getKey() == fileKey) return;
		
		// stop any playback that is in action
		if (mCurrentFilePlayer != null) {
			if (mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.stop();
			
			throwStopEvent(mCurrentFilePlayer);
			
			mCurrentFilePlayer.releaseMediaPlayer();
			mCurrentFilePlayer = null;
		}
		
		fileKey = fileKey < 0 ? mPlaylist.get(0).getKey() : fileKey;
        
		for (JrFile file : mPlaylist) {
			if (file.getKey() != fileKey) continue;
		
			JrFilePlayer filePlayer = new JrFilePlayer(mContext, file);
			filePlayer.addOnJrFileCompleteListener(this);
			filePlayer.addOnJrFilePreparedListener(this);
			filePlayer.addOnJrFileErrorListener(this);
			filePlayer.initMediaPlayer();
			filePlayer.seekTo(startPos < 0 ? filePlayer.getCurrentPosition() : startPos);
			mCurrentFilePlayer = filePlayer;
			throwChangeEvent(mCurrentFilePlayer);
			return;
		}
	}
	
	public void startAt(int fileKey, int startPos) {
		seekTo(fileKey, startPos);
		if (mCurrentFilePlayer != null && mCurrentFilePlayer.isPlaying()) return;
		mCurrentFilePlayer.prepareMediaPlayer(); // prepare async to not block main thread
	}
	
	public boolean resume() {
		if (mCurrentFilePlayer == null) {
			if (mFileKey == -1) return false;
			
			seekTo(mFileKey);
			return true;
		}
		
		if (!mCurrentFilePlayer.isMediaPlayerCreated()) {
			mCurrentFilePlayer.initMediaPlayer();
			mCurrentFilePlayer.prepareMediaPlayer();
			return true;
		}
		
		startFilePlayback(mCurrentFilePlayer);
		return true;
	}

	private void startFilePlayback(JrFilePlayer mediaPlayer) {
		mCurrentFilePlayer = mediaPlayer;
		
		mediaPlayer.setVolume(mVolume);
		mediaPlayer.start();
		
		mFileKey = mediaPlayer.getFile().getKey();
		
		JrFile nextFile = mediaPlayer.getFile().getNextFile();
		if (nextFile == null && mIsRepeating)
			nextFile = mPlaylist.get(0);
		
        if (nextFile != null)
        	prepareNextFile(nextFile);
        
        // Throw events after asynchronous calls have started
        throwChangeEvent(mCurrentFilePlayer);
        for (OnNowPlayingStartListener listener : mOnNowPlayingStartListeners)
        	listener.onNowPlayingStart(this, mCurrentFilePlayer);
	}
	
	private void prepareNextFile(JrFile nextFile) {
		mNextFilePlayer = new JrFilePlayer(mContext, nextFile);
		
		BackgroundFilePreparer backgroundFilePreparer = new BackgroundFilePreparer(mCurrentFilePlayer, mNextFilePlayer);
    	if (mBackgroundFilePreparerThread != null && mBackgroundFilePreparerThread.isAlive()) mBackgroundFilePreparerThread.interrupt();
    	mBackgroundFilePreparerThread = new Thread(backgroundFilePreparer);
    	mBackgroundFilePreparerThread.setName("Thread to prepare next file");
    	mBackgroundFilePreparerThread.setPriority(Thread.MIN_PRIORITY);
    	mBackgroundFilePreparerThread.start();
	}
	
	public void pause() {
		if (mCurrentFilePlayer == null) return;
		
		if (mCurrentFilePlayer.isPlaying()) mCurrentFilePlayer.pause();
		throwStopEvent(mCurrentFilePlayer);
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
				prepareNextFile(mPlaylist.get(0));
			} else {
				if (mBackgroundFilePreparerThread != null && mBackgroundFilePreparerThread.isAlive()) mBackgroundFilePreparerThread.interrupt();
				if (mNextFilePlayer != null) mNextFilePlayer.releaseMediaPlayer();
				mNextFilePlayer = null;
			}
		}
	}
	
	public boolean isRepeating() {
		return mIsRepeating;
	}
	
	/* End playlist control */
	
	public JrFilePlayer getCurrentFilePlayer() {
		return mCurrentFilePlayer;
	}
	
	public List<JrFile> getPlaylist() {
		return Collections.unmodifiableList(mPlaylist);
	}

	/* Event handlers */
	@Override
	public void onJrFilePrepared(JrFilePlayer mediaPlayer) {
		if (mediaPlayer.isPlaying()) return;
		
		startFilePlayback(mediaPlayer);
	}
	
	@Override
	public void onJrFileComplete(JrFilePlayer mediaPlayer) {
		throwStopEvent(mediaPlayer);
		
		mediaPlayer.releaseMediaPlayer();
		
		if (mNextFilePlayer == null) {
			if (mediaPlayer.getFile().getNextFile() == null) return;
			
			mNextFilePlayer = new JrFilePlayer(mContext, mediaPlayer.getFile().getNextFile());
		}
		
		mNextFilePlayer.addOnJrFileCompleteListener(this);
		mNextFilePlayer.addOnJrFileErrorListener(this);
		if (!mNextFilePlayer.isPrepared()) {
			mNextFilePlayer.addOnJrFilePreparedListener(this);
			mNextFilePlayer.prepareMediaPlayer();
			return;
		}
		
		startFilePlayback(mNextFilePlayer);
	}
	
	@Override
	public boolean onJrFileError(JrFilePlayer mediaPlayer, int what, int extra) {
		LoggerFactory.getLogger(JrPlaylistController.class).error("JR File error - " + what + " - " + extra);
		pause();
		
		for (OnPlaylistStateControlErrorListener listener : mOnPlaylistStateControlErrorListeners) {
			if (listener.onPlaylistStateControlError(this, mediaPlayer)) return true;
		}
		
		return false;
	}
	/* End event handlers */
	
	/* Listener callers */
	private void throwChangeEvent(JrFilePlayer filePlayer) {
		for (OnNowPlayingChangeListener listener : mOnNowPlayingChangeListeners)
			listener.onNowPlayingChange(this, filePlayer);
	}
	
	private void throwStopEvent(JrFilePlayer filePlayer) {
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
		
		if (mBackgroundFilePreparerThread != null && mBackgroundFilePreparerThread.isAlive())
			mBackgroundFilePreparerThread.interrupt();
	}
}
